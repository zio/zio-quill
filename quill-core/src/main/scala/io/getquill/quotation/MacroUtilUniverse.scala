package io.getquill.quotation

import io.getquill.{IdiomContext, Quoted}

import scala.reflect.api.Universe
import scala.reflect.macros.whitebox.Context

trait MacroUtilBase extends MacroUtilUniverse {
  val c: Context
  type Uni = c.universe.type
  // NOTE: u needs to be lazy otherwise sets value from c before c can be initialized by higher level classes
  lazy val u: Uni = c.universe
}

trait MacroUtilUniverse {
  type Uni <: Universe
  val u: Uni
  import u.{Block => _, Constant => _, Function => _, Ident => _, If => _, _}

  object QuotedType {
    def unapply(tpe: Type): Option[Type] =
      paramOf(tpe, typeOf[Quoted[Any]])
  }

  object QueryType {
    def unapply(tpe: Type): Option[Type] =
      paramOf(tpe, typeOf[io.getquill.Query[Any]])
  }

  object BatchType {
    def unapply(tpe: Type): Option[Type] =
      paramOf(tpe, typeOf[io.getquill.BatchAction[_]])
  }

  // Note: These will not match if they are not existential
  object ActionType {
    object Insert {
      def unapply(tpe: Type): Option[Type] =
        paramOf(tpe, typeOf[io.getquill.Insert[_]])
    }
    object Update {
      def unapply(tpe: Type): Option[Type] =
        paramOf(tpe, typeOf[io.getquill.Update[_]])
    }
    object Delete {
      def unapply(tpe: Type): Option[Type] =
        paramOf(tpe, typeOf[io.getquill.Delete[_]])
    }
  }

  object TypeSigParam {
    def unapply(tpe: Type): Option[Type] =
      tpe.typeSymbol.typeSignature.typeParams match {
        case head :: _ => Some(head.typeSignature)
        case Nil          => None
      }
  }

  def parseQueryType(tpe: Type): Option[IdiomContext.QueryType] = {
    println(s"Trying to match: ${show(tpe)}")
    tpe match {
      case QuotedType(tpe)        => parseQueryType(tpe)
      case BatchType(tpe)         => parseQueryType(tpe)
      case QueryType(_)         => Some(IdiomContext.QueryType.Select)
      case ActionType.Insert(_) => Some(IdiomContext.QueryType.Insert)
      case ActionType.Update(_) => Some(IdiomContext.QueryType.Update)
      case ActionType.Delete(_) => Some(IdiomContext.QueryType.Delete)
      case _                      => None
    }
  }

  def paramOf(tpe: Type, of: Type, maxDepth: Int = 10): Option[Type] =
    // println(s"### Attempting to check paramOf ${tpe} assuming it is a ${of}")
    tpe match {
      case _ if (maxDepth == 0) =>
        throw new IllegalArgumentException(s"Max Depth reached with type: ${tpe}")
      case _ if (!(tpe <:< of)) =>
        // println(s"### ${tpe} is not a ${of}")
        None
      case _ if (tpe =:= typeOf[Nothing] || tpe =:= typeOf[Any]) =>
        // println(s"### ${tpe} is Nothing or Any")
        None
      case TypeRef(_, _, List(arg)) =>
        // println(s"### ${tpe} is a TypeRef whose arg is ${arg}")
        Some(arg)
      case TypeSigParam(param) =>
        // println(s"### ${tpe} is a type signature whose type is ${param}")
        Some(param)
      case _ =>
        val base = tpe.baseType(of.typeSymbol)
        // println(s"### Going to base type for ${tpe} for expected base type ${of}")
        paramOf(base, of, maxDepth - 1)
    }

}
