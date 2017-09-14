package io.getquill

trait NamingStrategy {
  def table(s: String): String = default(s)
  def column(s: String): String = default(s)
  def default(s: String): String
}

trait CompositeNamingStrategy extends NamingStrategy {
  protected val elements: List[NamingStrategy]

  override def default(s: String) =
    elements.foldLeft(s)((s, n) => n.default(s))

  override def table(s: String) =
    elements.foldLeft(s)((s, n) => n.table(s))

  override def column(s: String) =
    elements.foldLeft(s)((s, n) => n.column(s))
}

case class CompositeNamingStrategy2[N1 <: NamingStrategy, N2 <: NamingStrategy](
  n1: N1, n2: N2
)
  extends CompositeNamingStrategy {
  override protected val elements = List(n1, n2)
}

case class CompositeNamingStrategy3[N1 <: NamingStrategy, N2 <: NamingStrategy, N3 <: NamingStrategy](
  n1: N1, n2: N2, n3: N3
)
  extends CompositeNamingStrategy {
  override protected val elements = List(n1, n2, n3)
}

case class CompositeNamingStrategy4[N1 <: NamingStrategy, N2 <: NamingStrategy, N3 <: NamingStrategy, N4 <: NamingStrategy](
  n1: N1, n2: N2, n3: N3, n4: N4
)
  extends CompositeNamingStrategy {
  override protected val elements = List(n1, n2, n3, n4)
}

object NamingStrategy {

  def apply[N1 <: NamingStrategy](n1: N1): N1 =
    n1

  def apply[N1 <: NamingStrategy, N2 <: NamingStrategy](
    n1: N1, n2: N2
  ): CompositeNamingStrategy2[N1, N2] =
    new CompositeNamingStrategy2(n1, n2)

  def apply[N1 <: NamingStrategy, N2 <: NamingStrategy, N3 <: NamingStrategy](
    n1: N1, n2: N2, n3: N3
  ): CompositeNamingStrategy3[N1, N2, N3] =
    new CompositeNamingStrategy3(n1, n2, n3)

  def apply[N1 <: NamingStrategy, N2 <: NamingStrategy, N3 <: NamingStrategy, N4 <: NamingStrategy](
    n1: N1, n2: N2, n3: N3, n4: N4
  ): CompositeNamingStrategy4[N1, N2, N3, N4] =
    new CompositeNamingStrategy4(n1, n2, n3, n4)

  private[getquill] def apply(strategies: List[NamingStrategy]) = {
    new CompositeNamingStrategy {
      override protected val elements = strategies
    }
  }
}

trait Literal extends NamingStrategy {
  override def default(s: String) = s
}
object Literal extends Literal

trait Escape extends NamingStrategy {
  override def default(s: String) =
    s""""$s""""
}
object Escape extends Escape

trait UpperCase extends NamingStrategy {
  override def default(s: String) =
    s.toUpperCase
}
object UpperCase extends UpperCase

trait LowerCase extends NamingStrategy {
  override def default(s: String) =
    s.toLowerCase
}
object LowerCase extends LowerCase

trait SnakeCase extends NamingStrategy {

  override def default(s: String) =
    (s.toList match {
      case c :: tail => c.toLower +: snakeCase(tail)
      case Nil       => Nil
    }).mkString

  private def snakeCase(s: List[Char]): List[Char] =
    s match {
      case c :: tail if c.isUpper => List('_', c.toLower) ++ snakeCase(tail)
      case c :: tail              => c +: snakeCase(tail)
      case Nil                    => Nil
    }
}
object SnakeCase extends SnakeCase

trait CamelCase extends NamingStrategy {

  override def default(s: String) =
    camelCase(s.toList).mkString

  private def camelCase(s: List[Char]): List[Char] =
    s match {
      case '_' :: Nil         => Nil
      case '_' :: '_' :: tail => camelCase('_' :: tail)
      case '_' :: c :: tail   => c.toUpper +: camelCase(tail)
      case c :: tail          => c +: camelCase(tail)
      case Nil                => Nil
    }
}
object CamelCase extends CamelCase

trait PluralizedTableNames extends NamingStrategy {
  override def default(s: String) = s
  override def table(s: String) =
    if (s.endsWith("s")) s
    else s + "s"
}
object PluralizedTableNames extends PluralizedTableNames

trait PostgresEscape extends Escape {
  override def column(s: String) = if (s.startsWith("$")) s else super.column(s)
}
object PostgresEscape extends PostgresEscape

trait MysqlEscape extends NamingStrategy {
  override def table(s: String) = quote(s)
  override def column(s: String) = quote(s)
  override def default(s: String) = s
  private def quote(s: String) = s"`$s`"
}
object MysqlEscape extends MysqlEscape
