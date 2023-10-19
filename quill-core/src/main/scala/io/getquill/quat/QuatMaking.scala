package io.getquill.quat

import io.getquill.quotation.{MacroUtilUniverse, MacroUtilBase}

import java.lang.reflect.Method
import io.getquill.{Embedded, Udt}
import io.getquill.util.{Messages, OptionalTypecheck}

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.reflect.api.Universe
import scala.reflect.macros.whitebox.Context

object QuatMaking {
  sealed trait IgnoreDecoders
  case object IgnoreDecoders extends IgnoreDecoders
}

trait QuatMaking extends QuatMakingBase with MacroUtilBase {
  val c: Context
  override type Uni = c.universe.type
  // NOTE: u needs to be lazy otherwise sets value from c before c can be initialized by higher level classes
  override lazy val u: Uni = c.universe

  import u.{Block => _, Constant => _, Function => _, Ident => _, If => _, _}
  import collection.mutable.HashMap;

  def onlyDecoder: Boolean = true

  val cachedEncoderLookups: HashMap[Type, Boolean] = HashMap();
  def existsEncoderFor(tpe: Type): Boolean =
    cachedEncoderLookups.get(tpe) match {
      case Some(value) =>
        value
      case None =>
        val lookup =
          (
            OptionalTypecheck(c)(q"implicitly[${c.prefix}.Encoder[$tpe]]"),
            OptionalTypecheck(c)(q"implicitly[${c.prefix}.Decoder[$tpe]]"),
            OptionalTypecheck(c)(q"implicitly[io.getquill.quat.QuatMaking.IgnoreDecoders]")
          ) match {
            case (Some(_), Some(_), _) => true
            case (Some(_), None, _)    => true
            case (None, Some(_), None) =>
              true // if there is a only a decoder available if the switch to IgnoreDecoders is enabled...
            case (None, Some(_), Some(_)) =>
              false // don't use it (e.g. for spark where there is a decoder for everything)
            case (None, None, _) => false
          }
        cachedEncoderLookups.put(tpe, lookup)
        lookup
    }

  val cachedQuats: HashMap[Type, Quat] = HashMap()
  override def inferQuat(tpe: u.Type): Quat =
    cachedQuats.get(tpe) match {
      case Some(value) =>
        value
      case None =>
        val quat = super.inferQuat(tpe)
        cachedQuats.put(tpe, quat)
        quat
    }
}

/**
 * Only runtime, only for Direct entity Types. Do not support things with
 * generic parameters like Query[T] etc... This is needed because the
 * quill-core/quill-sql JS use Scala JS which does not support TypeTags via the
 * Dynamic Query API, (the Macro API is fine to use them).
 */
object RuntimeEntityQuat {
  def apply[T](implicit ct: ClassTag[T]): Quat =
    forClassTopLevel(ct.runtimeClass)

  object AnyVal {
    def unapply(cls: Class[_]): Boolean =
      if (cls.getClasses.contains(classOf[AnyVal]))
        true
      else
        false
  }

  object Embedded {
    def unapply(cls: Class[_]): Option[List[Method]] =
      if (cls.getInterfaces.contains(classOf[Embedded]))
        cls match {
          case CaseClass(methods) => Some(methods)
          case _                  => None
        }
      else
        None
  }

  object CaseClass {
    // Common methods to exclude from object fields
    val exclude = classOf[Product].getMethods.map(_.getName).toSet ++ classOf[Object].getMethods.map(_.getName).toSet

    def unapply(cls: Class[_]): Option[List[Method]] =
      if (cls.getInterfaces.contains(classOf[Product])) {
        val methods = cls.getMethods
          .filter(r => r.getName() != "copy" && !r.getName.startsWith("copy$default") && !exclude.contains(r.getName))
          .toList
        Some(methods)
      } else {
        None
      }
  }

  object Tuple {
    def unapply(cls: Class[_]): Boolean =
      cls.getName.startsWith("scala.Tuple")
  }

  def forClass(cls: Class[_]): Quat =
    cls match {
      case AnyVal() => Quat.Value
      case Embedded(methods) =>
        Quat.Product(cls.getName.split('.').last, methods.map(m => (m.getName, forClass(m.getReturnType))))
      // If we are here we are already inside of a product which means if we are not a embedded, we have to be value-level
      case _ => Quat.Value
    }

  def forClassTopLevel(cls: Class[_]): Quat =
    cls match {
      case AnyVal() => Quat.Value
      // Embedded object can be a top-level entity
      case Embedded(methods) =>
        Quat.Product(cls.getName.split('.').last, methods.map(m => (m.getName, forClass(m.getReturnType))))
      case Tuple() =>
        throw new IllegalArgumentException("Tuple are not supported with Dynamic Query Schemas.")
      case CaseClass(methods) =>
        Quat.Product(cls.getName.split('.').last, methods.map(m => (m.getName, forClass(m.getReturnType))))
      case _ =>
        Quat.Value
    }

}

abstract class TypeTaggedQuatMaking extends QuatMakingBase {
  import scala.reflect.runtime.universe
  override type Uni = scala.reflect.runtime.universe.type
  // need to cast this to u.type otherwise 'existsEncoderFor' consumers will not be able to use scala.reflect.runtime.universe
  override val u: scala.reflect.runtime.universe.type = universe

  def existsEncoderFor(tpe: universe.Type): Boolean = quatValueTypes.contains(tpe)
  def quatValueTypes: List[universe.Type]
}

trait QuatMakingBase extends MacroUtilUniverse {
  type Uni <: Universe
  val u: Uni
  import u.{Block => _, Constant => _, Function => _, Ident => _, If => _, _}

  def existsEncoderFor(tpe: Type): Boolean

  def inferQuat(tpe: Type): Quat = {

    def nonGenericMethods(tpe: Type) =
      tpe.members.sorted
        .filter(m =>
          m.isPublic
            && m.owner.name.toString != "Any"
            && m.owner.name.toString != "Object"
        )
        .map { param =>
          (
            param.name.toString,
            // Look up the parameter only if needed. This is typically an expensive operation
            if (!param.isParameter) param.typeSignature else param.typeSignature.asSeenFrom(tpe, tpe.typeSymbol)
          )
        }
        .toList

    def caseClassConstructorArgs(tpe: Type) = {
      val constructor =
        tpe.members.collect {
          case m: MethodSymbol if m.isPrimaryConstructor => m
        }.head

      // Note. One one constructor param list is supported due to Quat Generation Specifics. This is already the case in most situations.
      constructor.paramLists(0).map { param =>
        (
          param.name.toString,
          if (!param.isParameter) param.typeSignature else param.typeSignature.asSeenFrom(tpe, tpe.typeSymbol)
        )
      }
    }

    object ArbitraryBaseType {
      def unapply(tpe: Type): Option[(String, List[(String, Type)])] =
        if (tpe.widen.typeSymbol.isClass)
          Some((tpe.widen.typeSymbol.name.toString, nonGenericMethods(tpe.widen)))
        else
          None
    }

    object CaseClassBaseType {
      def unapply(tpe: Type): Option[(String, List[(String, Type)])] =
        if (tpe.widen.typeSymbol.isClass && tpe.widen.typeSymbol.asClass.isCaseClass)
          Some((tpe.widen.typeSymbol.name.toString, caseClassConstructorArgs(tpe.widen)))
        else
          None
    }

    object Signature {
      def unapply(tpe: Type) =
        Some(tpe.typeSymbol.typeSignature)
    }

    object OptionType {
      def unapply(tpe: Type) =
        if (isOptionType(tpe))
          Some(innerOptionParam(tpe, None))
        else
          None
    }

    object Deoption {
      def unapply(tpe: Type) =
        if (isOptionType(tpe))
          Some(innerOptionParam(tpe, None))
        else
          Some(tpe)
    }

    object Param {
      def unapply(tpe: Type) =
        if (tpe.typeSymbol.isParameter || tpe.typeSymbol.isAbstract)
          Some(tpe)
        else
          None
    }

    object RealTypeBounds {
      def unapply(tpe: Type) =
        tpe match {
          case TypeBounds(lower, upper) if (upper != null && !(upper =:= typeOf[Any])) =>
            Some((lower, upper))
          case _ =>
            None
        }
    }

    object BooleanType {
      def unapply(tpe: Type): Option[Type] =
        if (isType[Boolean](tpe.finalResultType)) {
          Some(tpe)
        } else {
          None
        }
    }

    object DefiniteValue {
      def unapply(tpe: Type): Option[Type] =
        // UDTs (currently only used by cassandra) are created as tables even though there is an encoder for them.
        if (tpe <:< typeOf[Udt])
          None
        else if (isType[AnyVal](tpe))
          Some(tpe)
        else if (existsEncoderFor(tpe))
          Some(tpe)
        else
          None
    }

    def parseTopLevelType(tpe: Type): Quat =
      tpe match {
        case BooleanType(tpe) =>
          Quat.BooleanValue

        case OptionType(BooleanType(innerParam)) =>
          Quat.BooleanValue

        case DefiniteValue(tpe) =>
          Quat.Value

        // If it is a query type, recurse into it
        case QueryType(tpe) =>
          parseType(tpe)

        // For cases where the type is actually a parameter with type bounds
        // and the upper bound is not final, assume that polymorphism is being used
        // and that the user wants to extend a class e.g.
        // trait Spirit { def grade: Int }
        // case class Gin(grade: Int) extends Spirit
        // def is80Prof[T <: Spirit] = quote { (spirit: Query[Spirit]) => spirit.filter(_.grade == 80) }
        // run(is80Proof(query[Gin]))
        // When processing is80Prof, we assume that Spirit is actually a base class to be extended
        case Param(Signature(RealTypeBounds(lower, Deoption(upper))))
            if (!upper.typeSymbol.isFinal && !existsEncoderFor(tpe)) =>
          parseType(upper, true) match {
            case p: Quat.Product => p.copy(tpe = Quat.Product.Type.Abstract)
            case other           => other
          }

        case Param(RealTypeBounds(lower, Deoption(upper))) if (!upper.typeSymbol.isFinal && !existsEncoderFor(tpe)) =>
          parseType(upper, true) match {
            case p: Quat.Product => p.copy(tpe = Quat.Product.Type.Abstract)
            case other           => other
          }

        case other =>
          parseType(other)
      }

    /*
     * Quat parsing has a top-level type parsing function and then secondary function which is recursed. This is because
     * things like type boundaries (e.g.  type-bounds types (e.g. Query[T &lt;: BaseType]) should only be checked once
     * at the top level.
     */
    def parseType(tpe: Type, boundedInterfaceType: Boolean = false): Quat =
      tpe match {
        case BooleanType(tpe) =>
          Quat.BooleanValue

        case OptionType(BooleanType(_)) =>
          Quat.BooleanValue

        case DefiniteValue(tpe) =>
          Quat.Value

        // This will happens for val-parsing situations e.g. where you have val (a,b) = (Query[A],Query[B]) inside a quoted block.
        // In this situations, the CaseClassBaseType should activate first and recurse which will then hit this case clause.
        case QueryType(tpe) =>
          parseType(tpe)

        // If the type is optional, recurse
        case OptionType(innerParam) =>
          parseType(innerParam)

        case _ if (isNone(tpe)) =>
          Quat.Null

        // For other types of case classes (and if there does not exist an encoder for it)
        // the exception to that is a cassandra UDT that we treat like an encodable entity even if it has a parsed type
        case CaseClassBaseType(name, fields) if !existsEncoderFor(tpe) || tpe <:< typeOf[Udt] =>
          Quat.Product(
            name.split('.').last,
            fields.map { case (fieldName, fieldType) => (fieldName, parseType(fieldType)) }
          )

        // If we are already inside a bounded type, treat an arbitrary type as a interface list
        case ArbitraryBaseType(name, fields) if (boundedInterfaceType) =>
          Quat.Product(
            name.split('.').last,
            fields.map { case (fieldName, fieldType) => (fieldName, parseType(fieldType)) }
          )

        // Is it a generic or does it have any generic parameters that have not been filled (e.g. is T not filled in Option[T] ?)
        case Param(tpe) =>
          Quat.Generic

        // Otherwise it's a terminal value
        case _ =>
          Messages.trace(s"Could not infer SQL-type of ${tpe}, assuming it is a Unknown Quat.")
          Quat.Unknown
      }

    parseTopLevelType(tpe)
  }

  @tailrec
  private[getquill] final def innerOptionParam(tpe: Type, maxDepth: Option[Int]): Type = tpe match {
    case TypeRef(_, cls, List(arg))
        if (cls.isClass && cls.asClass.fullName == "scala.Option") && maxDepth.forall(_ > 0) =>
      innerOptionParam(arg, maxDepth.map(_ - 1))
    // If it's not a ref-type but an Option, convert to a ref-type and reprocess
    // also since Nothing is a subtype of everything need to know to stop searching once Nothing
    // has been reached (since we have not gone inside anything, do not decrement the depth here).
    case _ if (isOptionType(tpe) && !(tpe =:= typeOf[Nothing])) && maxDepth.forall(_ > 0) =>
      innerOptionParam(tpe.baseType(typeOf[Option[Any]].typeSymbol), maxDepth)
    // Otherwise we have gotten to the actual type inside the nesting. Check what it is.
    case other => other
  }

  def isNone(tpe: Type) = {
    val era = tpe.erasure
    era =:= typeOf[None.type]
  }

  // Note. Used in other places beside here where None needs to be included in option type.
  def isOptionType(tpe: Type) = {
    val era = tpe.erasure
    era =:= typeOf[Option[Any]] || era =:= typeOf[Some[Any]] || era =:= typeOf[None.type]
  }

  private[getquill] def isType[T](tpe: Type)(implicit tt: TypeTag[T]) =
    tpe <:< tt.tpe
}
