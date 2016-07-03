package io.getquill.sources

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.util.Messages._
import io.getquill.Embedded

trait EncodingMacro {
  val c: Context

  import c.universe._

  sealed trait Value
  case class OptionValue(value: Value) extends Value
  case class SimpleValue(ast: Ast, encoding: c.Tree, optionEncoding: c.Tree) extends Value

  sealed trait NestedValue extends Value

  object NestedValue {
    def unapply(value: Value): Option[(c.Type, List[List[Value]])] =
      value match {
        case CaseClassValue(tpe, params) => Some((tpe, params))
        case EmbeddedValue(tpe, params)  => Some((tpe, params))
        case other                       => None
      }
  }

  case class CaseClassValue(tpe: c.Type, params: List[List[Value]]) extends NestedValue
  case class EmbeddedValue(tpe: c.Type, params: List[List[Value]]) extends NestedValue

  protected def encoding[T](ast: Ast, inferEncoding: Type => Option[Tree])(implicit t: WeakTypeTag[T]): Value =
    encoding(ast, t.tpe, inferEncoding)

  private def encoding(ast: Ast, typ: Type, inferEncoding: Type => Option[Tree]): Value =
    (inferEncoding(typ), inferEncoding(optionType(c.WeakTypeTag(typ))), ast) match {
      case (_, _, ast) if (typ <:< c.weakTypeOf[Option[Any]]) =>
        OptionValue(encoding(ast, typ.typeArgs.head, inferEncoding))
      case (Some(encoding), Some(optionEncoding), ast) =>
        SimpleValue(ast, encoding, optionEncoding)
      case (None, _, ast) if (isEmbeddable(typ)) =>
        EmbeddedValue(typ, valuesForCaseClass(typ, ast, inferEncoding))
      case (None, _, ast) if (typ.typeSymbol.asClass.isCaseClass) =>
        CaseClassValue(typ, valuesForCaseClass(typ, ast, inferEncoding))
      case other =>
        c.fail(s"Source doesn't know how to decode '$ast: $typ'")
    }

  private def isEmbeddable(typ: c.Type) =
    typ.typeSymbol.asClass.isCaseClass &&
      (typ <:< c.weakTypeOf[Embedded] ||
        typ <:< c.weakTypeOf[WrappedType] ||
        typ.typeSymbol.fullName.toString.startsWith("scala.Tuple"))

  private def optionType[T](implicit t: WeakTypeTag[T]) =
    c.weakTypeOf[Option[T]]

  private def valuesForCaseClass(typ: Type, ast: Ast, inferEncoding: Type => Option[Tree]) =
    valuesForConstructor(typ, caseClassConstructor(typ), ast, inferEncoding)

  private def valuesForConstructor(typ: Type, constructor: MethodSymbol, ast: Ast, inferEncoding: Type => Option[Tree]) =
    constructor.paramLists.map(_.map {
      param =>
        val paramType = param.typeSignature.asSeenFrom(typ, typ.typeSymbol)
        val nestedAst =
          if (typ <:< c.weakTypeOf[WrappedType])
            ast
          else
            Property(ast, param.name.decodedName.toString)
        encoding(nestedAst, paramType, inferEncoding)
    })

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if (m.isPrimaryConstructor) => m
    }.headOption.get // a case class always has a primary constructor
}
