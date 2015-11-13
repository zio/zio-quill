package io.getquill.naming

trait NamingStrategy {
  def apply(s: String): String
}

trait Literal extends NamingStrategy {
  override def apply(s: String) = s
}
object Literal extends Literal

trait Escape extends NamingStrategy {
  override def apply(s: String) =
    s""""$s""""
}
object Escape extends Escape

trait UpperCase extends NamingStrategy {
  override def apply(s: String) =
    s.toUpperCase
}
object UpperCase extends UpperCase

trait LowerCase extends NamingStrategy {
  override def apply(s: String) =
    s.toLowerCase
}
object LowerCase extends LowerCase

trait SnakeCase extends NamingStrategy {

  override def apply(s: String) =
    (s.toList match {
      case c :: tail => c.toLower +: snakeCase(tail)
      case Nil       => Nil
    }).mkString

  private def snakeCase(s: List[Char]): List[Char] =
    s match {
      case c :: tail if (c.isUpper) => List('_', c.toLower) ++ snakeCase(tail)
      case c :: tail                => c +: snakeCase(tail)
      case Nil                      => Nil
    }
}
object SnakeCase extends SnakeCase

trait CamelCase extends NamingStrategy {

  override def apply(s: String) =
    calmelCase(s.toList).mkString

  private def calmelCase(s: List[Char]): List[Char] =
    s match {
      case '_' :: Nil         => Nil
      case '_' :: '_' :: tail => calmelCase('_' :: tail)
      case '_' :: c :: tail   => c.toUpper +: calmelCase(tail)
      case c :: tail          => c +: calmelCase(tail)
      case Nil                => Nil
    }
}
object CamelCase extends CamelCase
