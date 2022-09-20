package io.getquill.context.sql.norm

import io.getquill.ast._
import io.getquill.context.sql._
import io.getquill.sql.norm.{InContext, QueryLevel, SelectPropertyProtractor, StatelessQueryTransformer}
import io.getquill.ast.PropertyOrCore
import io.getquill.norm.PropertyMatryoshka
import io.getquill.quat.Quat

class ExpandSelection(from: List[FromContext]) {

  /** For external things to potentially use ExpandSelection */
  def ofTop(values: List[SelectValue], topLevelQuat: Quat): List[SelectValue] =
    apply(values, QueryLevel.Top(topLevelQuat))

  def ofSubselect(values: List[SelectValue]): List[SelectValue] =
    apply(values, QueryLevel.Inner)

  private[norm] def apply(values: List[SelectValue], level: QueryLevel): List[SelectValue] =
    values.flatMap(apply(_, level))

  implicit class AliasOp(alias: Option[String]) {
    def concatWith(str: String): Option[String] =
      alias.orElse(Some("")).map(v => s"${v}${str}")
  }

  private[norm] def apply(value: SelectValue, level: QueryLevel): List[SelectValue] = {
    def concatOr(concatA: Option[String], concatB: String)(or: Option[String]) =
      if (!level.isTop)
        concatA.concatWith(concatB)
      else
        or

    value match {
      // Assuming there's no case class or tuple buried inside or a property i.e. if there were,
      // the beta reduction would have unrolled them already
      case SelectValue(ast @ PropertyOrCore(), alias, concat) =>
        // Alternate quat that can be used for top level SelectValue elements in case it is a single identifier that is abstract
        val alternateQuat =
          level match {
            case QueryLevel.Top(quat) => Some(quat)
            case _                    => None
          }

        val exp = SelectPropertyProtractor(from)(ast, alternateQuat)
        exp.map {
          case (p: Property, Nil) =>
            // If the quat-path is nothing and there is some pre-existing alias (e.g. if we came from a case-class or quat)
            // the use that. Otherwise the selection is of an individual element so use the element name (before the rename)
            // as the alias.
            alias match {
              case None =>
                SelectValue(p, Some(p.prevName.getOrElse(p.name)), concat)
              case Some(value) =>
                SelectValue(p, Some(value), concat)
            }
          case (p: Property, path) =>
            // Append alias headers (i.e. _1,_2 from tuples and field names foo,bar from case classes) to the
            // value of the Quat path
            SelectValue(p, concatOr(alias, path.mkString)(path.lastOption), concat)
          case (other, _) =>
            SelectValue(other, alias, concat)
        }
      case SelectValue(Tuple(values), alias, concat) =>
        values.zipWithIndex.flatMap { case (ast, i) =>
          val label = s"_${i + 1}"
          // Go into the select values, if the level is Top we need to go TopUnwrapped since the top-level
          // Quat doesn't count anymore. If level=Inner then it's the same.
          apply(SelectValue(ast, concatOr(alias, label)(Some(label)), concat), level.withoutTopQuat)
        }
      case SelectValue(CaseClass(_, fields), alias, concat) =>
        fields.flatMap { case (name, ast) =>
          // Go into the select values, if the level is Top we need to go TopUnwrapped since the top-level
          // Quat doesn't count anymore. If level=Inner then it's the same.
          apply(SelectValue(ast, concatOr(alias, name)(Some(name)), concat), level.withoutTopQuat)
        }
      // Direct infix select, etc...
      case other => List(other)
    }
  }
}

/*
 * Much of what this does is documented in PRs here:
 * https://github.com/zio/zio-quill/pull/1920 and here:
 * https://github.com/zio/zio-quill/pull/2381 and here:
 * https://github.com/zio/zio-quill/pull/2420
 */
object ExpandNestedQueries extends StatelessQueryTransformer {

  protected override def apply(q: SqlQuery, level: QueryLevel): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        val selection = new ExpandSelection(q.from)(q.select, level)
        val out       = expandNested(q.copy(select = selection)(q.quat), level)
        out
      case other =>
        super.apply(q, level)
    }

  case class FlattenNestedProperty(from: List[FromContext]) {
    val inContext = InContext(from)

    def apply(p: Ast): Ast =
      p match {
        case p @ PropertyMatryoshka(inner, path, renameables) =>
          val isSubselect       = inContext.isSubselect(p)
          val propsAlreadyFixed = renameables.forall(_ == Renameable.Fixed)
          val isPropertyRenamed = p.prevName.isDefined
          val renameable =
            if (isPropertyRenamed || isSubselect || propsAlreadyFixed)
              Renameable.Fixed
            else
              Renameable.ByStrategy

          // If it is a sub-select or a renamed property, do not apply the strategy to the property
          if (isSubselect)
            Property.Opinionated(inner, path.mkString, renameable, Visibility.Visible)
          else
            Property.Opinionated(inner, path.last, renameable, Visibility.Visible)

        case other => other
      }

    def inside(ast: Ast) =
      Transform(ast) { case p: Property =>
        apply(p)
      }
  }

  protected override def expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        val flattenNestedProperty = FlattenNestedProperty(from)
        val newFroms              = q.from.map(expandContextFlattenOns(_, flattenNestedProperty))

        def distinctIfNotTopLevel(values: List[SelectValue]) =
          if (level.isTop)
            values
          else
            values.distinct

        /*
         * In sub-queries, need to make sure that the same field/alias pair is not selected twice
         * which is possible when aliases are used. For example, something like this:
         *
         * case class Emb(id: Int, name: String)
         * case class Parent(id: Int, name: String, emb: Emb)
         * case class GrandParent(id: Int, par: Parent)
         * val q = quote { query[GrandParent].map(g => g.par).distinct.map(p => (p.name, p.emb, p.id, p.emb.id)).distinct.map(tup => (tup._1, tup._2, tup._3, tup._4)).distinct }
         * Will cause double-select inside the innermost subselect:
         * SELECT DISTINCT theParentName AS theParentName, id AS embid, theName AS embtheName, id AS id, id AS embid FROM GrandParent g
         * Note how embid occurs twice? That's because (p.emb.id, p.emb) are expanded into (p.emb.id, p.emb.id, e.emb.name).
         *
         * On the other hand if the query is top level we need to make sure not to do this deduping or else the encoders won't work since they rely on clause positions
         * For example, something like this:
         * val q = quote { query[GrandParent].map(g => g.par).distinct.map(p => (p.name, p.emb, p.id, p.emb.id)) }
         * Would normally expand to this:
         * SELECT p.theParentName, p.embid, p.embtheName, p.id, p.embid FROM ...
         * Note now embed occurs twice? We need to maintain this because the second element of the output tuple
         * (p.name, p.emb, p.id, p.emb.id) needs the fields p.embid, p.embtheName in that precise order in the selection
         * or they cannot be encoded.
         */
        val distinctSelects =
          distinctIfNotTopLevel(select)

        val distinctKind =
          q.distinct match {
            case DistinctKind.DistinctOn(props) =>
              DistinctKind.DistinctOn(props.map(p => flattenNestedProperty.inside(p)))
            case other => other
          }

        q.copy(
          select = distinctSelects.map(sv => sv.copy(ast = flattenNestedProperty.inside(sv.ast))),
          from = newFroms,
          where = where.map(flattenNestedProperty.inside(_)),
          groupBy = groupBy.map(flattenNestedProperty.inside(_)),
          orderBy = orderBy.map(ob => ob.copy(ast = flattenNestedProperty.inside(ob.ast))),
          limit = limit.map(flattenNestedProperty.inside(_)),
          offset = offset.map(flattenNestedProperty.inside(_)),
          distinct = distinctKind
        )(q.quat)
    }

  def expandContextFlattenOns(s: FromContext, flattenNested: FlattenNestedProperty): FromContext = {
    def expandContextRec(s: FromContext): FromContext =
      s match {
        case QueryContext(q, alias) =>
          QueryContext(apply(q, QueryLevel.Inner), alias)
        case JoinContext(t, a, b, on) =>
          JoinContext(t, expandContextRec(a), expandContextRec(b), flattenNested.inside(on))
        case FlatJoinContext(t, a, on) =>
          FlatJoinContext(t, expandContextRec(a), flattenNested.inside(on))
        case _: TableContext | _: InfixContext => s
      }

    expandContextRec(s)
  }
}
