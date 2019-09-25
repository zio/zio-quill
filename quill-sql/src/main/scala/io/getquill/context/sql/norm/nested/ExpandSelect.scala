package io.getquill.context.sql.norm.nested

import io.getquill.NamingStrategy
import io.getquill.ast.Property
import io.getquill.context.sql.SelectValue
import io.getquill.util.Interpolator
import io.getquill.util.Messages.TraceType.NestedQueryExpansion

import scala.collection.mutable.LinkedHashSet
import io.getquill.context.sql.norm.nested.Elements._
import io.getquill.ast._
import io.getquill.norm.BetaReduction

/**
 * Takes the `SelectValue` elements inside of a sub-query (if a super/sub-query constrct exists) and flattens
 * them from a nested-hiearchical structure (i.e. tuples inside case classes inside tuples etc..) into
 * into a single series of top-level select elements where needed. In cases where a user wants to select an element
 * that contains an entire tuple (i.e. a sub-tuple of the outer select clause) we pull out the entire tuple
 * that is being selected and leave it to the tokenizer to flatten later.
 *
 * The part about this operation that is tricky is if there are situations where there are infix clauses
 * in a sub-query representing an element that has not been selected by the query-query but in order to ensure
 * the SQL operation has the same meaning, we need to keep track for it. For example:
 * <pre><code>
 *   val q = quote {
 *     query[Person].map(p => (infix"DISTINCT ON (${p.other})".as[Int], p.name, p.id)).map(t => (t._2, t._3))
 *   }
 *   run(q)
 *   // SELECT p._2, p._3 FROM (SELECT DISTINCT ON (p.other), p.name AS _2, p.id AS _3 FROM Person p) AS p
 * </code></pre>
 * Since `DISTINCT ON` significantly changes the behavior of the outer query, we need to keep track of it
 * inside of the inner query. In order to do this, we need to keep track of the location of the infix in the inner query
 * so that we can reconstruct it. This is why the `OrderedSelect` and `DoubleOrderedSelect` objects are used.
 * See the notes on these classes for more detail.
 *
 * See issue #1597 for more details and another example.
 */
private class ExpandSelect(selectValues: List[SelectValue], references: LinkedHashSet[Property], strategy: NamingStrategy) {
  val interp = new Interpolator(NestedQueryExpansion, 3)
  import interp._

  object TupleIndex {
    def unapply(s: String): Option[Int] =
      if (s.matches("_[0-9]*"))
        Some(s.drop(1).toInt - 1)
      else
        None
  }

  object MultiTupleIndex {
    def unapply(s: String): Boolean =
      if (s.matches("(_[0-9]+)+"))
        true
      else
        false
  }

  val select =
    selectValues.zipWithIndex.map {
      case (value, index) => OrderedSelect(index, value)
    }

  def expandColumn(name: String, renameable: Renameable): String =
    renameable.fixedOr(name)(strategy.column(name))

  def apply: List[SelectValue] =
    trace"Expanding Select values: $selectValues into references: $references" andReturn {

      def expandReference(ref: Property): OrderedSelect =
        trace"Expanding: $ref from $select" andReturn {

          def expressIfTupleIndex(str: String) =
            str match {
              case MultiTupleIndex() => Some(str)
              case _                 => None
            }

          def concat(alias: Option[String], idx: Int) =
            Some(s"${alias.getOrElse("")}_${idx + 1}")

          val orderedSelect = ref match {
            case pp @ Property(ast: Property, TupleIndex(idx)) =>
              trace"Reference is a sub-property of a tuple index: $idx. Walking inside." andReturn
                expandReference(ast) match {
                  case OrderedSelect(o, SelectValue(Tuple(elems), alias, c)) =>
                    trace"Expressing Element $idx of $elems " andReturn
                    OrderedSelect(o :+ idx, SelectValue(elems(idx), concat(alias, idx), c))
                  case OrderedSelect(o, SelectValue(ast, alias, c)) =>
                    trace"Appending $idx to $alias " andReturn
                    OrderedSelect(o, SelectValue(ast, concat(alias, idx), c))
                }
            case pp @ Property.Opinionated(ast: Property, name, renameable, visible) =>
              trace"Reference is a sub-property. Walking inside." andReturn
                expandReference(ast) match {
                  case OrderedSelect(o, SelectValue(ast, nested, c)) =>
                    // Alias is the name of the column after the naming strategy
                    // The clauses in `SqlIdiom` that use `Tokenizer[SelectValue]` select the
                    // alias field when it's value is Some(T).
                    // Technically the aliases of a column should not be using naming strategies
                    // but this is an issue to fix at a later date.

                    // In the current implementation, aliases we add nested tuple names to queries e.g.
                    // SELECT foo from
                    // SELECT x, y FROM (SELECT foo, bar, red, orange FROM baz JOIN colors)
                    // Typically becomes SELECT foo _1foo, _1bar, _2red, _2orange when
                    // this kind of query is the result of an applicative join that looks like this:
                    // query[baz].join(query[colors]).nested
                    // this may need to change based on how distinct appends table names instead of just tuple indexes
                    // into the property path.

                    trace"...inside walk completed, continuing to return: " andReturn
                    OrderedSelect(o, SelectValue(
                      // Note: Pass invisible properties to be tokenized by the idiom, they should be excluded there
                      Property.Opinionated(ast, name, renameable, visible),
                      // Skip concatonation of invisible properties into the alias e.g. so it will be
                      Some(s"${nested.getOrElse("")}${expandColumn(name, renameable)}")
                    ))
                }
            case pp @ Property(_, TupleIndex(idx)) =>
              trace"Reference is a tuple index: $idx from $select." andReturn
                select(idx) match {
                  case OrderedSelect(o, SelectValue(ast, alias, c)) =>
                    OrderedSelect(o, SelectValue(ast, concat(alias, idx), c))
                }
            case pp @ Property.Opinionated(_, name, renameable, visible) =>
              select match {
                case List(OrderedSelect(o, SelectValue(cc: CaseClass, alias, c))) =>
                  // Currently case class element name is not being appended. Need to change that in order to ensure
                  // path name uniqueness in future.
                  val ((_, ast), index) = cc.values.zipWithIndex.find(_._1._1 == name) match {
                    case Some(v) => v
                    case None    => throw new IllegalArgumentException(s"Cannot find element $name in $cc")
                  }
                  trace"Reference is a case class member: " andReturn
                    OrderedSelect(o :+ index, SelectValue(ast, Some(expandColumn(name, renameable)), c))
                case List(OrderedSelect(o, SelectValue(i: Ident, _, c))) =>
                  trace"Reference is an identifier: " andReturn
                    OrderedSelect(o, SelectValue(Property.Opinionated(i, name, renameable, visible), Some(name), c))
                case other =>
                  trace"Reference is unidentified: $other returning:" andReturn
                    OrderedSelect(Integer.MAX_VALUE, SelectValue(Ident.Opinionated(name, visible), Some(expandColumn(name, renameable)), false))
              }
          }

          // For certain very large queries where entities are unwrapped and then re-wrapped into CaseClass/Tuple constructs,
          // the actual row-types can contain Tuple/CaseClass values. For this reason. They need to be beta-reduced again.
          val normalizedOrderedSelect = orderedSelect.copy(selectValue =
            orderedSelect.selectValue.copy(ast =
              BetaReduction(orderedSelect.selectValue.ast)))

          trace"Expanded $ref into $orderedSelect then Normalized to $normalizedOrderedSelect" andReturn
            normalizedOrderedSelect
        }

      def deAliasWhenUneeded(os: OrderedSelect) =
        os match {
          case OrderedSelect(_, sv @ SelectValue(Property(Ident(_), propName), Some(alias), _)) if (propName == alias) =>
            trace"Detected select value with un-needed alias: $os removing it:" andReturn
              os.copy(selectValue = sv.copy(alias = None))
          case _ => os
        }

      references.toList match {
        case Nil => select.map(_.selectValue)
        case refs => {
          // elements first need to be sorted by their order in the select clause. Since some may map to multiple
          // properties when expanded, we want to maintain this order of properties as a secondary value.
          val mappedRefs =
            refs
              // Expand all the references to properties that we have selected in the super query
              .map(expandReference)
              // Once all the recursive calls of expandReference are done, remove the alias if it is not needed.
              // We cannot do this because during recursive calls, the aliases of outer clauses are used for inner ones.
              .map(deAliasWhenUneeded(_))

          trace"Mapped Refs: $mappedRefs".andLog()

          // are there any selects that have infix values which we have not already selected? We need to include
          // them because they could be doing essential things e.g. RANK ... ORDER BY
          val remainingSelectsWithInfixes =
            trace"Searching Selects with Infix:" andReturn
              new FindUnexpressedInfixes(select)(mappedRefs)

          implicit val ordering: scala.math.Ordering[List[Int]] = new scala.math.Ordering[List[Int]] {
            override def compare(x: List[Int], y: List[Int]): Int =
              (x, y) match {
                case (head1 :: tail1, head2 :: tail2) =>
                  val diff = head1 - head2
                  if (diff != 0) diff
                  else compare(tail1, tail2)
                case (Nil, Nil)   => 0 // List(1,2,3) == List(1,2,3)
                case (head1, Nil) => -1 // List(1,2,3) < List(1,2)
                case (Nil, head2) => 1 // List(1,2) > List(1,2,3)
              }
          }

          val sortedRefs =
            (mappedRefs ++ remainingSelectsWithInfixes).sortBy(ref => ref.order) //(ref.order, ref.secondaryOrder)

          sortedRefs.map(_.selectValue)
        }
      }
    }
}

object ExpandSelect {
  def apply(selectValues: List[SelectValue], references: LinkedHashSet[Property], strategy: NamingStrategy): List[SelectValue] =
    new ExpandSelect(selectValues, references, strategy).apply
}
