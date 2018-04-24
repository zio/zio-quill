package io.getquill.context.spark.norm

import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.ast._
import io.getquill.context.sql.FlattenSqlQuery
import io.getquill.context.sql.FromContext
import io.getquill.context.sql.InfixContext
import io.getquill.context.sql.JoinContext
import io.getquill.context.sql.QueryContext
import io.getquill.context.sql.SelectValue
import io.getquill.context.sql.SetOperationSqlQuery
import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.TableContext
import io.getquill.context.sql.UnaryOperationSqlQuery
import io.getquill.context.sql.FlatJoinContext

/**
 * Spark requires nested objects to be expanded in tupled form in order to be properly encoded.
 * In order to do this, we have to find the needed Entity by looking up the field identity
 * and then expand it's fields. When a Spark Dataset is encoded via "liftDataset",
 * we attempt to do the same thing by pulling out the case class (and it's fields)
 * from the ScalarValueLift.
 */
object ExpandEntityIds {

  private def replaceSelect(v: SelectValue, searchState: SearchState, idx: Int, isNested: Boolean): SelectValue = {
    val newAst = diveReplaceAst(v.ast, searchState)

    def newAlias = Some(v.alias.getOrElse((s"_${idx}")))
    // Make sure if what was returned was a tuple or case class, that tuple or case class actually has fields
    // otherwise fall back to the original entity.
    newAst match {
      case Tuple(props) if (props.size > 0) => v.copy(ast = newAst, alias = newAlias)
      case CaseClass(props) if (props.size > 0) => v.copy(ast = newAst, alias = newAlias)
      case other if (!other.isInstanceOf[Tuple] && !other.isInstanceOf[CaseClass]) => v.copy(ast = newAst)
      case _ => v
    }
  }

  private def diveReplaceAst(a: Ast, searchState: SearchState): Ast = {
    def diveReplaceRecurse(a: Ast): Ast =
      a match {
        case Tuple(values)     => Tuple(values.map(diveReplaceRecurse))
        case CaseClass(values) => CaseClass(values.map({ case (k, v) => (k, diveReplaceRecurse(v)) }))
        case id @ Ident(name) =>
          if (searchState.contains(name))
            Tuple(searchState(name).fields)
          else
            id
        case other => other
      }

    diveReplaceRecurse(a)
  }

  private def gatherSubstates(s: SqlQuery): (SqlQuery, SearchState) =
    s match {
      case q: FlattenSqlQuery => {
        val contextsAndStates = q.from.map(processSingleFromContext)
        (
          q.copy(from = contextsAndStates.map({ case (newFrom, _) => newFrom })),
          contextsAndStates.map({ case (_, state) => state }).fold(NotFound)(_ merge _)
        )
      }
      case so @ SetOperationSqlQuery(a, _, b) => {
        val (queryA, stateA) = processQuerySelects(a)
        val (queryB, stateB) = processQuerySelects(b)
        (so.copy(a = queryA, b = queryB), stateA merge stateB)
      }
      case uo @ UnaryOperationSqlQuery(_, q) => {
        val (query, state) = processQuerySelects(q)
        (uo.copy(q = query), state)
      }
    }

  private def processSingleFromContext(s: FromContext): (FromContext, SearchState) =
    s match {
      case qc @ QueryContext(q, queryAlias) => {
        val (newQuery, state) = processQuerySelects(q, true)
        val newState =
          newQuery match {
            case fq: FlattenSqlQuery =>
              FoundEntityIds(queryAlias, fq.select.collect({
                case SelectValue(_, Some(alias), _) => alias
              }))
            case _ => state
          }
        (qc.copy(query = newQuery), newState)
      }
      case j @ JoinContext(_, a, b, _) => {
        val (contextA, stateA) = processSingleFromContext(a)
        val (contextB, stateB) = processSingleFromContext(b)
        (j.copy(a = contextA, b = contextB), stateA merge stateB)
      }
      case f @ FlatJoinContext(_, a, _) => {
        val (context, state) = processSingleFromContext(a)
        (f.copy(a = context), state)
      }
      // Star-selection expansion for spark is not selected for entity query
      // the proper way to use spark is via liftQuery
      case t @ TableContext(e, alias) => (t, NotFound)
      case inf @ InfixContext(i, alias) => {
        (inf, searchInfix(i, alias))
      }
    }

  // Search infixes for a dataframe definition. This is a best-effort process.
  def searchInfix(i: Infix, id: String): SearchState = InfixExtractor(i, id)

  private def processQuerySelects(q: SqlQuery, nested: Boolean = false): (SqlQuery, SearchState) = {
    val output = gatherSubstates(q)
    output match {
      case (f: FlattenSqlQuery, searchState: FoundEntityIds) => {
        val newQuery = f.copy(select =
          f.select.zipWithIndex.map({
            case (sv, idx) => replaceSelect(sv, searchState, idx, nested)
          }))
        (retuplize(newQuery), searchState)
      }
      case (s: SetOperationSqlQuery, searchState: FoundEntityIds) => (s, searchState)
      case (u: UnaryOperationSqlQuery, searchState: FoundEntityIds) => (u, searchState)
      case _ => (q, NotFound)
    }
  }

  /**
   * Spark expects nested case classes to be expressed in a <code>((fields...) var, (fields...) var) var</code>
   * form. In particular, one case class cannot be within another case classes's ast or the format will be invalid,
   * we have to wrap the inner one in a tuple beforehand.
   * This function does exactly that:
   *
   * <pre>
   * SELECT (a.i, a.j) ta, (a.i, a.j) tb tha, foo FROM ... ->  // Invalid format for Spark
   * SELECT ((a.i, a.j) ta), ((a.i, a.j) tb) tha, FROM ...   // Valid format for Spark
   * </pre>
   */
  private def surroundCaseClassesWithTuple(sv: SelectValue) = {
    def surroundCaseClassesWithTupleInternal(cc: Ast): Ast = {
      cc match {
        case cc @ CaseClass(_) => Tuple(List(CaseClass(cc.values.map({ case (k, v) => (k, surroundCaseClassesWithTupleInternal(v)) }))))
        case Tuple(props)      => Tuple(props.map(surroundCaseClassesWithTupleInternal))
        case other             => other
      }
    }
    sv.copy(ast = surroundCaseClassesWithTupleInternal(sv.ast))
  }

  /**
   * This function does two things. The first is to expand a tuple/caseclass which is the only argument
   * in the selects since spark does not support this kind of construction:
   *
   * <pre>
   * SELECT ((fooa, foob) foobar) FROM ... -> // This becomes SELECT ((fooa, foob) foobar) _1 FROM ... which in invalid
   * SELECT (fooa, foob) foobar FROM ...
   * </pre>
   *
   * In cases where they are multiple arguments however since these steps typically run recursively, you
   * start with an object that has already been detupleized so it needs to be retuplized correctly now.
   *
   * <pre>
   *
   * // In previous steps they went "SELECT ((fooa, foob) foo) FROM ..." to "SELECT (fooa, foob) foo FROM ..."
   * // which is a typical result of using Ad-Hoc case classes.
   * SELECT ((fooa, foob) foo) fo, ((baza, bazb) baz) ba foba, ... FROM ... -> // which is invalid
   *
   * // So now you need to surround the case classes with tuples so that Spark decodes them correctly:
   * SELECT (((fooa, foob) foo) fo), (((baza, bazb) baz) ba) foba, ... FROM ...
   *
   * By performing both off these steps (recursively if any subqueries are involved), the output
   * of the <code>SelectValue</code>s will be specified correctly for spark decoding to work.
   * </pre>
   */
  def retuplize(q: SqlQuery): SqlQuery =
    q match {
      case f: FlattenSqlQuery => {
        val detupleized = detupleize(f)
        detupleized.copy(select = detupleized.select.map(surroundCaseClassesWithTuple))
      }
      case SetOperationSqlQuery(a, op, b) => SetOperationSqlQuery(retuplize(a), op, retuplize(b))
      case UnaryOperationSqlQuery(op, q)  => UnaryOperationSqlQuery(op, retuplize(q))
    }

  /**
   * If a query has a single select-value which is a tuple, 'unwrap' that into a set of
   * select-values representing the individual fields.
   *
   * <pre>
   * SELECT (foo, bar) FROM ... ->  // Which later becomes SELECT (foo, bar) _1 FROM ... which is invalid
   * SELECT foo, bar FROM ...
   * </pre>
   *
   * When the selection criteria has nested tuples that need to be reformatted, also do that.
   * <pre>
   * SELECT ((a, b) foo, bar) FROM ... ->  // Which later becomes SELECT ((a, b), bar) _1 FROM ... which is invalid
   * SELECT (a, b) foo, bar FROM ...
   * </pre>
   *
   * For three levels of nesting, it looks like this:
   * <pre>
   * SELECT (((x, y), b) foo, bar) FROM ... ->  // Which later becomes SELECT (((x, y), b), bar) _1 FROM ... which is invalid
   * SELECT ((x, y) a, b) foo, bar FROM ...
   * </pre>
   */
  private def detupleize(q: FlattenSqlQuery): FlattenSqlQuery = {
    def expandSelectValue(selectValue: SelectValue): List[SelectValue] =
      selectValue match {
        case SelectValue(cc @ CaseClass(_), _, concat) =>
          cc.values
            .map({ case (k, v) => SelectValue(v, Some(k), concat) })

        case SelectValue(Tuple(props), _, _) => props.map({
          // if the select is a property that is selecting, add the property's name for the alias
          case Property(ast, name) => SelectValue(Property(ast, name), Some(name), false)
          // this case shouldn't happen but fallback to just returning the property if it does
          case other               => SelectValue(other, None, false)
        })

        case other => List(other)
      }

    q.select match {
      case selectValue :: Nil => q.copy(select = expandSelectValue(selectValue))
      case _                  => q
    }
  }

  def apply(q: SqlQuery): SqlQuery = processQuerySelects(q) match {
    case (q, _) => retuplize(q)
  }
}
