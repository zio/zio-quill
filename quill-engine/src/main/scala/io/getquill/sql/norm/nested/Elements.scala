package io.getquill.context.sql.norm.nested

import io.getquill.PseudoAst
import io.getquill.context.sql.SelectValue

object Elements {
  /**
   * In order to be able to reconstruct the original ordering of elements inside of a select clause,
   * we need to keep track of their order, not only within the top-level select but also it's order
   * within any possible tuples/case-classes that in which it is embedded.
   * For example, in the query:
   * <pre><code>
   *   query[Person].map(p => (p.id, (p.name, p.age))).nested
   *   // SELECT p.id, p.name, p.age FROM (SELECT x.id, x.name, x.age FROM person x) AS p
   * </code></pre>
   * Since the `p.name` and `p.age` elements are selected inside of a sub-tuple, their "order" is
   * `List(2,1)` and `List(2,2)` respectively as opposed to `p.id` whose "order" is just `List(1)`.
   *
   * This class keeps track of the values needed in order to perform do this.
   */
  case class OrderedSelect(order: List[Int], selectValue: SelectValue) extends PseudoAst {
    override def toString: String = s"[${order.mkString(",")}]${selectValue}"
  }
  object OrderedSelect {
    def apply(order: Int, selectValue: SelectValue) =
      new OrderedSelect(List(order), selectValue)
  }
}
