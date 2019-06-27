package io.getquill

import org.atteo.evo.inflector.English

trait Pluralize extends NamingStrategy {
  override def table(s: String): String = English.plural(s)
  override def default(s: String): String = s
}
object Pluralize extends Pluralize
