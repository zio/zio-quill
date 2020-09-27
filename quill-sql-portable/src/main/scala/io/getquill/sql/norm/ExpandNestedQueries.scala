package io.getquill.context.sql.norm

import io.getquill.ast._
import io.getquill.context.sql._
import io.getquill.sql.norm.{ InContext, SelectPropertyProtractor, StatelessQueryTransformer }
import io.getquill.ast.PropertyOrCore
import io.getquill.norm.PropertyMatroshka

class ExpandSelection(from: List[FromContext]) {

  def apply(values: List[SelectValue]): List[SelectValue] =
    values.flatMap(apply(_))

  implicit class AliasOp(alias: Option[String]) {
    def concatWith(str: String): Option[String] =
      alias.orElse(Some("")).map(v => s"${v}${str}")
  }

  private def apply(value: SelectValue): List[SelectValue] = {
    value match {
      // Assuming there's no case class or tuple buried inside or a property i.e. if there were,
      // the beta reduction would have unrolled them already
      case SelectValue(ast @ PropertyOrCore(), alias, concat) =>
        val exp = SelectPropertyProtractor(from)(ast)
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
            SelectValue(p, alias.concatWith(path.mkString), concat)
          case (other, _) =>
            SelectValue(other, alias, concat)
        }
      case SelectValue(Tuple(values), alias, concat) =>
        values.zipWithIndex.flatMap {
          case (ast, i) =>
            apply(SelectValue(ast, alias.concatWith(s"_${i + 1}"), concat))
        }
      case SelectValue(CaseClass(fields), alias, concat) =>
        fields.flatMap {
          case (name, ast) =>
            apply(SelectValue(ast, alias.concatWith(name), concat))
        }
      // Direct infix select, etc...
      case other => List(other)
    }
  }
}

object ExpandNestedQueries extends StatelessQueryTransformer {

  protected override def apply(q: SqlQuery, isTopLevel: Boolean = false): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        expandNested(q.copy(select = new ExpandSelection(q.from)(q.select))(q.quat), isTopLevel)
      case other =>
        super.apply(q, isTopLevel)
    }

  case class FlattenNestedProperty(from: List[FromContext]) {
    val inContext = InContext(from)

    def apply(p: Ast): Ast = {
      p match {
        case p @ PropertyMatroshka(inner, path) =>
          val isSubselect = inContext.isSubselect(p)
          val renameable =
            if (p.prevName.isDefined || isSubselect)
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
    }

    def inside(ast: Ast) =
      Transform(ast) {
        case p: Property => apply(p)
      }
  }

  protected override def expandNested(q: FlattenSqlQuery, isTopLevel: Boolean): FlattenSqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        val flattenNestedProperty = FlattenNestedProperty(from)
        val newFroms = q.from.map(expandContextFlattenOns(_, flattenNestedProperty))

        def distinctIfNotTopLevel(values: List[SelectValue]) =
          if (isTopLevel)
            values
          else
            values.distinct

        /*
         * In sub-queries, need to make sure that the same field/alias pair is not selected twice
         * which is possible when aliases are used. For example, something like this:
         *
         * case class Emb(id: Int, name: String) extends Embedded
         * case class Parent(id: Int, name: String, emb: Emb) extends Embedded
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

        q.copy(
          select = distinctSelects.map(sv => sv.copy(ast = flattenNestedProperty.inside(sv.ast))),
          from = newFroms,
          where = where.map(flattenNestedProperty.inside(_)),
          groupBy = groupBy.map(flattenNestedProperty.inside(_)),
          orderBy = orderBy.map(ob => ob.copy(ast = flattenNestedProperty.inside(ob.ast))),
          limit = limit.map(flattenNestedProperty.inside(_)),
          offset = offset.map(flattenNestedProperty.inside(_))
        )(q.quat)
    }

  def expandContextFlattenOns(s: FromContext, flattenNested: FlattenNestedProperty): FromContext = {
    def expandContextRec(s: FromContext): FromContext =
      s match {
        case QueryContext(q, alias) =>
          QueryContext(apply(q, false), alias)
        case JoinContext(t, a, b, on) =>
          JoinContext(t, expandContextRec(a), expandContextRec(b), flattenNested.inside(on))
        case FlatJoinContext(t, a, on) =>
          FlatJoinContext(t, expandContextRec(a), flattenNested.inside(on))
        case _: TableContext | _: InfixContext => s
      }

    expandContextRec(s)
  }
}
