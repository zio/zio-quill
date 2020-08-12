package io.getquill.sql.norm

import io.getquill.ast.{ Ast, Ident, Property, Renameable }
import io.getquill.ast.Visibility.{ Hidden, Visible }
import io.getquill.context.sql.{ FromContext, TableContext }
import io.getquill.norm.PropertyMatroshka
import io.getquill.quat.Quat
import io.getquill.ast.Core

case class SelectPropertyProtractor(from: List[FromContext]) {

  private def refersToEntity(ast: Ast) = {
    val tables = from.collect { case TableContext(entity, alias) => alias }
    ast match {
      case Ident(v, _)                       => tables.contains(v)
      case PropertyMatroshka(Ident(v, _), _) => tables.contains(v)
      case _                                 => false
    }
  }

  /*
  * Properties that do not belong to an entity i.e. where the 'from' is not
  * a TableContext but rather a QueryContext (so idents being selected from are not
  * direct tables) need to be visible and fixed since they should not be affected by naming strategies.
  */
  def freezeNonEntityProps(p: Ast, isEntity: Boolean): Ast = {
    def freezeEntityPropsRecurse(p: Ast): Ast =
      p match {
        case Property.Opinionated(ast, name, r, v) =>
          Property.Opinionated(freezeEntityPropsRecurse(ast), name, Renameable.Fixed, Visible)
        case other =>
          other
      }
    if (!isEntity) freezeEntityPropsRecurse(p) else p
  }

  def apply(ast: Ast): List[(Ast, List[String])] = {
    ast match {
      case id @ Core() =>
        val isEntity = refersToEntity(id)
        id.quat match {
          case p: Quat.Product =>
            ProtractQuat(isEntity)(p, id).map { case (prop, path) => (freezeNonEntityProps(prop, isEntity), path) }
          case _ =>
            List(id).map(p => (freezeNonEntityProps(p, isEntity), List.empty))
        }
      // Assuming a property contains only an Ident, Infix or Constant at this point
      // and all situations where there is a case-class, tuple, etc... inside have already been beta-reduced
      case prop @ PropertyMatroshka(id @ Core(), _) =>
        val isEntity = refersToEntity(id)
        prop.quat match {
          case p: Quat.Product =>
            ProtractQuat(isEntity)(p, prop).map { case (prop, path) => (freezeNonEntityProps(prop, isEntity), path) }
          case _ =>
            List(prop).map(p => (freezeNonEntityProps(p, isEntity), List.empty))
        }
      case other => List(other).map(p => (p, List.empty))
    }
  }

  /* Take a quat and project it out as nested properties with some core ast inside.
 * quat: CC(foo,bar:Quat(a,b)) with core id:Ident(x) =>
 *   List( Prop(id,foo) [foo], Prop(Prop(id,bar),a) [bar.a], Prop(Prop(id,bar),b) [bar.b] )
 */
  case class ProtractQuat(refersToEntity: Boolean) {
    def apply(quat: Quat.Product, core: Ast): List[(Property, List[String])] =
      applyInner(quat, core)

    def applyInner(quat: Quat.Product, core: Ast): List[(Property, List[String])] = {
      // Property (and alias path) should be visible unless we are referring directly to a TableContext
      // with an Entity that has embedded fields. In that case, only top levels should show since
      // we're selecting from an actual table and in that case, the embedded paths don't actually exist.
      val wholePathVisible = !refersToEntity

      // Assuming renames have been applied so the value of a renamed field will be the 2nd element
      def isPropertyRenameable(name: String) =
        if (quat.renames.find(_._2 == name).isDefined)
          Renameable.Fixed
        else
          Renameable.ByStrategy

      quat.fields.flatMap {
        case (name, child: Quat.Product) =>
          // Should not need this
          //val fieldName = quat.renames.find(_._1 == name).map(_._2).getOrElse(name)
          // TODO Quat once renames changed to LinkedHashSet change the lookup here to lookup from the hash for better perf
          applyInner(
            child,
            Property.Opinionated(
              core,
              // If the quat is renamed, create a property representing the renamed field, otherwise use the quat field for the property
              name,
              // Property is renameable if it is being directly selected from a table and there is no naming strategy selecting from it
              isPropertyRenameable(name),
              /* If the property represents a property of a Entity (i.e. we're selecting from an actual table,
               * then the entire projection of the Quat should be visible (since subsequent aliases will
               * be using the entire path.
               * Take: Bim(bid:Int, mam:Mam), Mam(mid:Int, mood:Int) extends Embedded
               * Here is an example:
               * SELECT g.mam FROM
               *    SELECT gim.bim: CC(bid:Int,mam:CC(mid:Int,mood:Int)) FROM g
               *
               * This needs to be projected into:
               * SELECT g.mammid, g.mammood FROM                         -- (2) so their selection of sub-properties from here is correct
               *    SELECT gim.mid AS mammid, gim.mood as mammood FROM g -- (1) for mamid and mammood need full quat path here...
               *
               * (See examples of this in ExpandNestedQueries multiple embedding levels series of tests. Also note that since sub-selection
               * is typically done from tuples, paths typically start with _1,_2 etc...)
               */
              if (wholePathVisible) Visible else Hidden
            )
          ).map {
              case (prop, path) =>
                (prop, name +: path)
            }
        case (name, _) =>
          // If the quat is renamed, create a property representing the renamed field, otherwise use the quat field for the property
          //val fieldName = quat.renames.find(_._1 == name).map(_._2).getOrElse(name)
          // The innermost entity of the quat. This is always visible since it is the actual column of the table
          List((Property.Opinionated(core, name, isPropertyRenameable(name), Visible), List(name)))
      }.toList
    }
  }
}
