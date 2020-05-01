package io.getquill.ast

object Implicits {
  implicit class AstOpsExt(body: Ast) {
    def +||+(other: Ast) = BinaryOperation(body, BooleanOperator.`||`, other)
    def +&&+(other: Ast) = BinaryOperation(body, BooleanOperator.`&&`, other)
    def +==+(other: Ast) = BinaryOperation(body, EqualityOperator.`==`, other)
    def +!=+(other: Ast) = BinaryOperation(body, EqualityOperator.`!=`, other)
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

object +!=+ {
  def unapply(a: Ast): Option[(Ast, Ast)] = {
    a match {
      case BinaryOperation(one, EqualityOperator.`!=`, two) => Some((one, two))
      case _ => None
    }
  }
}

object IsNotNullCheck {
  def apply(ast: Ast) = BinaryOperation(ast, EqualityOperator.`!=`, NullValue)

  def unapply(ast: Ast): Option[Ast] = {
    ast match {
      case BinaryOperation(cond, EqualityOperator.`!=`, NullValue) => Some(cond)
      case _ => None
    }
  }
}

object IsNullCheck {
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
    If(IsNotNullCheck(exists), `then`, NullValue)

  def unapply(ast: Ast) = ast match {
    case If(IsNotNullCheck(exists), t, NullValue) => Some((exists, t))
    case _                                        => None
  }
}

object IfExist {
  def apply(exists: Ast, `then`: Ast, otherwise: Ast) =
    If(IsNotNullCheck(exists), `then`, otherwise)

  def unapply(ast: Ast) = ast match {
    case If(IsNotNullCheck(exists), t, e) => Some((exists, t, e))
    case _                                => None
  }
}
