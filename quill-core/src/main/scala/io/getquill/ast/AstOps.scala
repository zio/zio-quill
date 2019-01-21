package io.getquill.ast

import io.getquill.norm.BetaReduction

object Implicits {
  implicit class AstOpsExt(body: Ast) {
    def reduce(reductions: (Ast, Ast)*) = BetaReduction(body, reductions: _*)
    def +||+(other: Ast) = BinaryOperation(body, BooleanOperator.`||`, other)
    def +&&+(other: Ast) = BinaryOperation(body, BooleanOperator.`&&`, other)
    def +==+(other: Ast) = BinaryOperation(body, EqualityOperator.`==`, other)
  }
}

object +||+ {
  def unapply(a: Ast): Option[(Ast, Ast)] = {
    a match {
      case BinaryOperation(one, BooleanOperator.`||`, two) => Some((one, two))
      case _ => None
    }
  }
}

object +&&+ {
  def unapply(a: Ast): Option[(Ast, Ast)] = {
    a match {
      case BinaryOperation(one, BooleanOperator.`&&`, two) => Some((one, two))
      case _ => None
    }
  }
}

object +==+ {
  def unapply(a: Ast): Option[(Ast, Ast)] = {
    a match {
      case BinaryOperation(one, EqualityOperator.`==`, two) => Some((one, two))
      case _ => None
    }
  }
}

object Exist {
  def apply(ast: Ast) = BinaryOperation(ast, EqualityOperator.`!=`, NullValue)

  def unapply(ast: Ast): Option[Ast] = {
    ast match {
      case BinaryOperation(cond, EqualityOperator.`!=`, NullValue) => Some(cond)
      case _ => None
    }
  }
}

object Empty {
  def apply(ast: Ast) = BinaryOperation(ast, EqualityOperator.`==`, NullValue)

  def unapply(ast: Ast): Option[Ast] = {
    ast match {
      case BinaryOperation(cond, EqualityOperator.`==`, NullValue) => Some(cond)
      case _ => None
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
