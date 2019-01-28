package io.getquill.ast

import io.getquill.ast.{ BinaryOperation => B }
import io.getquill.ast.{ EqualityOperator => EQ }
import io.getquill.ast.{ BooleanOperator => BO }
import io.getquill.norm.BetaReduction

object Implicits {
  implicit class AstOpsExt(body: Ast) {
    def reduce(reductions: (Ast, Ast)*) = BetaReduction(body, reductions: _*)
    def +||+(other: Ast) = B(body, BO.`||`, other)
    def +&&+(other: Ast) = B(body, BO.`&&`, other)
    def +==+(other: Ast) = B(body, EQ.`==`, other)
  }
}

object +||+ {
  def unapply(a: Ast): Option[(Ast, Ast)] = {
    a match {
      case B(one, BO.`||`, two) => Some((one, two))
      case _                    => None
    }
  }
}

object +&&+ {
  def unapply(a: Ast): Option[(Ast, Ast)] = {
    a match {
      case B(one, BO.`&&`, two) => Some((one, two))
      case _                    => None
    }
  }
}

object Exist {
  def apply(ast: Ast) = B(ast, EQ.`!=`, NullValue)

  def unapply(ast: Ast): Option[Ast] = {
    ast match {
      case BinaryOperation(cond, EQ.`!=`, NullValue) => Some(cond)
      case _                                         => None
    }
  }
}

object Empty {
  def apply(ast: Ast) = B(ast, EQ.`==`, NullValue)

  def unapply(ast: Ast): Option[Ast] = {
    ast match {
      case BinaryOperation(cond, EQ.`==`, NullValue) => Some(cond)
      case _                                         => None
    }
  }
}

object IfExistElseNull {
  def apply(exists: Ast, `then`: Ast) =
    If(Exist(exists), `then`, NullValue)

  def unapply(ast: Ast) = ast match {
    case If(Exist(exists), t, NullValue) => Some((exists, t))
    case _                               => None
  }
}
object IfExist {
  def apply(exists: Ast, `then`: Ast, otherwise: Ast) =
    If(Exist(exists), `then`, otherwise)

  def unapply(ast: Ast) = ast match {
    case If(Exist(exists), t, e) => Some((exists, t, e))
    case _                       => None
  }
}
