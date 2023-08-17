package io.getquill.context.sql.norm.nested

import io.getquill.context.sql.norm.nested.Elements._
import io.getquill.util.{Interpolator, TraceConfig}
import io.getquill.util.Messages.TraceType.NestedQueryExpansion
import io.getquill.ast._
import io.getquill.context.sql.SelectValue

/**
 * The challenge with appending infixes (that have not been used but are still
 * needed) back into the query, is that they could be inside of
 * tuples/case-classes that have already been selected, or inside of sibling
 * elements which have been selected. Take for instance a query that looks like
 * this: <pre><code> query[Person].map(p => (p.name, (p.id,
 * sql"foo(\${p.other})".as[Int]))).map(p => (p._1, p._2._1)) </code></pre> In
 * this situation, `p.id` which is the sibling of the non-selected infix has
 * been selected via `p._2._1` (whose select-order is List(1,0) to represent 1st
 * element in 2nd tuple. We need to add it's sibling infix.
 *
 * Or take the following situation: <pre><code> query[Person].map(p => (p.name,
 * (p.id, sql"foo(\${p.other})".as[Int]))).map(p => (p._1, p._2)) </code></pre>
 * In this case, we have selected the entire 2nd element including the infix. We
 * need to know that `P._2._2` does not need to be selected since `p._2` was.
 *
 * In order to do these things, we use the `order` property from `OrderedSelect`
 * in order to see which sub-sub-...-element has been selected. If `p._2` (that
 * has order `List(1)`) has been selected, we know that any infixes inside of it
 * e.g. `p._2._1` (ordering `List(1,0)`) does not need to be.
 */
class FindUnexpressedInfixes(select: List[OrderedSelect], traceConfig: TraceConfig) {
  val interp = new Interpolator(NestedQueryExpansion, traceConfig, 3)
  import interp._

  def apply(refs: List[OrderedSelect]) = {

    def pathExists(path: List[Int]) =
      refs.map(_.order).contains(path)

    def containsInfix(ast: Ast) =
      CollectAst.byType[Infix](ast).length > 0

    // build paths to every infix and see these paths were not selected already
    def findMissingInfixes(ast: Ast, parentOrder: List[Int]): List[(Ast, List[Int])] = {
      trace"Searching for infix: $ast in the sub-path $parentOrder".andLog()
      if (pathExists(parentOrder))
        trace"No infixes found" andContinue
          List()
      else
        ast match {
          case Tuple(values) =>
            values.zipWithIndex
              .filter(v => containsInfix(v._1))
              .flatMap { case (ast, index) =>
                findMissingInfixes(ast, parentOrder :+ index)
              }
          case CaseClass(_, values) =>
            values.zipWithIndex
              .filter(v => containsInfix(v._1._2))
              .flatMap { case ((_, ast), index) =>
                findMissingInfixes(ast, parentOrder :+ index)
              }
          case other if (containsInfix(other)) =>
            trace"Found unexpressed infix inside $other in $parentOrder".andLog()
            List((other, parentOrder))
          case _ =>
            List()
        }
    }

    select.flatMap { case OrderedSelect(o, sv) =>
      findMissingInfixes(sv.ast, o)
    }.map { case (ast, order) =>
      OrderedSelect(order, SelectValue(ast))
    }
  }
}
