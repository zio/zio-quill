package io.getquill.sql.norm

import io.getquill.ast.{Ast, Core, Ident, Property, Renameable}
import io.getquill.ast.Visibility.{Hidden, Visible}
import io.getquill.context.sql.{FlatJoinContext, FromContext, InfixContext, JoinContext, QueryContext, TableContext}
import io.getquill.norm.PropertyMatroshka
import io.getquill.quat.Quat
import io.getquill.sql.norm.InContext.{InContextType, InInfixContext, InQueryContext, InTableContext}

/**
 * Simple utility that checks if if an AST entity refers to a entity. It
 * traverses through the context types to find out what kind of context a
 * variable refers to. For example, in a query like:
 * {{{query[Pair].map(p => Pair(p.a, p.b)).distinct.map(p => (p.b, p.a))}}}
 *
 * Yielding SQL like this:
 * {{{SELECT p.b, p.a FROM (SELECT DISTINCT p.a, p.b FROM pair p) AS p}}}
 *
 * the inner p.a and p.b will have a TableContext while the outer p.b and p.a
 * will have a QueryContext.
 *
 * Note that there are some cases where the id of a field is not in a
 * FromContext at all. For example, in a query like this:
 * {{{query[Human].filter(h => query[Robot].filter(r => r.friend == h.id).nonEmpty)}}}
 *
 * Where the sql produced is something like this:
 * {{{SELECT h.id FROM human h WHERE EXISTS (SELECT r.* FROM robot r WHERE r.friend = h.id)}}}
 *
 * the field `r.friend`` is selected from a sub-query of an SQL operation (i.e.
 * `EXISTS (...)`) so a from-context of it will not exist at all. When deciding
 * which properties to treat as sub-select properties (e.g. if we want to make
 * sure NOT to apply a naming-schema on them) we need to take care to understand
 * that we may not know the FromContext that a property comes from since it may
 * not exist.
 */
case class InContext(from: List[FromContext]) {
  // Are we sure it is a subselect
  def isSubselect(ast: Ast) =
    contextReferenceType(ast) match {
      case Some(InQueryContext) => true
      case _                    => false
    }

  // Are we sure it is a table reference
  def isEntityReference(ast: Ast) =
    contextReferenceType(ast) match {
      case Some(InTableContext) => true
      case Some(InInfixContext) => true
      case _                    => false
    }

  def contextReferenceType(ast: Ast) = {
    val references = collectTableAliases(from)
    ast match {
      case Ident(v, _)                          => references.get(v)
      case PropertyMatroshka(Ident(v, _), _, _) => references.get(v)
      case _                                    => None
    }
  }

  private def collectTableAliases(contexts: List[FromContext]): Map[String, InContextType] =
    contexts.map {
      case c: TableContext             => Map(c.alias -> InTableContext)
      case c: QueryContext             => Map(c.alias -> InQueryContext)
      case c: InfixContext             => Map(c.alias -> InInfixContext)
      case JoinContext(_, a, b, _)     => collectTableAliases(List(a)) ++ collectTableAliases(List(b))
      case FlatJoinContext(_, from, _) => collectTableAliases(List(from))
    }.foldLeft(Map[String, InContextType]())(_ ++ _)
}
object InContext {
  sealed trait InContextType
  case object InTableContext extends InContextType
  case object InQueryContext extends InContextType
  case object InInfixContext extends InContextType
}

case class SelectPropertyProtractor(from: List[FromContext]) {
  val inContext = InContext(from)

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

  private def nonAbstractQuat(from: Quat, or: Option[Quat]) =
    (from, or) match {
      case (Quat.IsAbstract(), Some(value @ Quat.NotAbstract())) => value
      case _                                                     => from
    }

  /**
   * Turn product quats into case class asts e.g. Quat.Product(name:V,age:V) =>
   * CaseClass(name->p.name,age->p.age) `alternateQuat` is there in case it's a
   * top-level expansion and the identifier is generic or unknown (or an
   * abstract product quat) so we want to try and get information from the quat
   * from the [T] of the executeQuery[T] itself (similar to quatOf[T]) this
   * information is already supplied by higher level constructs.
   */
  def apply(ast: Ast, alternateQuat: Option[Quat]): List[(Ast, List[String])] =
    ast match {
      case id @ Core() =>
        // The quat is considered to be an entity (i.e. thing whose fields need to be renamed) if it is either:
        // a) Found in the table references (i.e. it's an actual table in the subselect) or...
        // b) We are selecting fields from an infix e.g. `sql"selectPerson()".as[Query[Person]]`
        val isEntity      = inContext.isEntityReference(id)
        val effectiveQuat = nonAbstractQuat(id.quat, alternateQuat)

        effectiveQuat match {
          case p: Quat.Product =>
            ProtractQuat(isEntity)(p, id).map { case (prop, path) => (freezeNonEntityProps(prop, isEntity), path) }
          case _ =>
            List(id).map(p => (freezeNonEntityProps(p, isEntity), List.empty))
        }
      // Assuming a property contains only an Ident, Infix or Constant at this point
      // and all situations where there is a case-class, tuple, etc... inside have already been beta-reduced
      case prop @ PropertyMatroshka(id @ Core(), _, _) =>
        val isEntity      = inContext.isEntityReference(id)
        val effectiveQuat = nonAbstractQuat(prop.quat, alternateQuat)

        effectiveQuat match {
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
  def apply(quat: Quat.Product, core: Ast): List[(Property, List[String])] = {
    val prot = applyInner(quat, core)
    prot
  }

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
        // val fieldName = quat.renames.find(_._1 == name).map(_._2).getOrElse(name)
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
             * Take: Bim(bid:Int, mam:Mam), Mam(mid:Int, mood:Int)
             * Here is an example:
             * SELECT g.mam FROM
             *    SELECT gim.bim: CC(bid:Int,mam:CC(mid:Int,mood:Int)) FROM g
             *
             * This needs to be projected into:
             * SELECT g.mammid, g.mammood FROM                         -- (2) so their selection of sub-properties from here is correct
             *    SELECT gim.mid AS mammid, gim.mood as mammood FROM g -- (1) for mammid and mammood need full quat path here...
             *
             * (See examples of this in ExpandNestedQueries multiple embedding levels series of tests. Also note that since sub-selection
             * is typically done from tuples, paths typically start with _1,_2 etc...)
             */
            if (wholePathVisible) Visible else Hidden
          )
        ).map { case (prop, path) =>
          (prop, name +: path)
        }
      case (name, _) =>
        // If the quat is renamed, create a property representing the renamed field, otherwise use the quat field for the property
        // val fieldName = quat.renames.find(_._1 == name).map(_._2).getOrElse(name)
        // The innermost entity of the quat. This is always visible since it is the actual column of the table
        List((Property.Opinionated(core, name, isPropertyRenameable(name), Visible), List(name)))
    }.toList
  }
}
