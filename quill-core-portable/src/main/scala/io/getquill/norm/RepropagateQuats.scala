package io.getquill.norm

import io.getquill.ast.{ Action, Assignment, Ast, ConcatMap, Filter, FlatJoin, FlatMap, GroupBy, Ident, Insert, Join, Map, OnConflict, Property, Query, Returning, ReturningGenerated, SortBy, StatelessTransformer, Update }
import io.getquill.quat.Quat
import io.getquill.quat.Quat.Product
import io.getquill.util.Interpolator
import io.getquill.util.Messages.TraceType
import io.getquill.quotation.QuatExceptionOps._

import scala.collection.mutable

object RepropagateQuats extends StatelessTransformer {
  import TypeBehavior.{ ReplaceWithReduction => RWR }
  val msg = "This is acceptable from dynamic queries."

  val interp = new Interpolator(TraceType.RepropagateQuats, 1)
  import interp._

  implicit class QuatExt(q: Quat) {
    def retypeFrom(other: Quat): Quat = {
      (q, other) match {
        case (Quat.BooleanValue, Quat.BooleanExpression) => Quat.BooleanValue
        case (Quat.BooleanExpression, Quat.BooleanValue) => Quat.BooleanValue
        case (Quat.Value, Quat.BooleanValue)             => Quat.BooleanValue
        case (Quat.Value, Quat.BooleanExpression)        => Quat.Value
        case (Quat.BooleanValue, Quat.Value)             => Quat.BooleanValue
        case (Quat.BooleanExpression, Quat.Value)        => Quat.Value
        case (me: Quat.Product, other: Quat.Product)     => me.retypeProduct(other)
        case (_, other)                                  => other
      }
    }
  }
  implicit class ProductQuatExt(q: Quat.Product) {
    import io.getquill.quat.LinkedHashMapOps._

    def retypeProduct(other: Quat.Product): Quat.Product = {
      val newFieldsIter =
        q.fields.outerZipWith(other.fields) {
          case (key, Some(thisQuat), Some(otherQuat)) => (key, thisQuat.retypeFrom(otherQuat))
          case (key, Some(value), None)               => (key, value)
          case (key, None, Some(value))               => (key, value)
        }
      val newFields = mutable.LinkedHashMap(newFieldsIter.toList: _*)
      val newTpe =
        if (q.tpe == Product.Type.Abstract || other.tpe == Product.Type.Abstract)
          Product.Type.Abstract
        else
          Product.Type.Concrete
      // Note, some extra renames from properties that don't exist could make it here.
      // Need to make sure to ignore extra ones when they are actually applied.
      Quat.Product(newFields).withRenames(other.renames).withType(newTpe)
    }
  }

  implicit class IdentExt(id: Ident) {
    def retypeQuatFrom(from: Quat) =
      id.copy(quat = id.quat.retypeFrom(from))
  }

  def applyBody(a: Ast, b: Ident, c: Ast)(f: (Ast, Ident, Ast) => Query) = {
    val ar = apply(a)
    val br = b.retypeQuatFrom(ar.quat)
    val cr = BetaReduction(c, RWR, b -> br)
    trace"Repropagate ${a.quat.suppress(msg)} from ${a} into:" andReturn f(ar, br, apply(cr))
  }

  override def apply(e: Query): Query = {
    //println("==================== Before ==================")
    //println(io.getquill.util.Messages.qprint(e))

    val o =
      e match {
        case Filter(a, b, c) => applyBody(a, b, c)(Filter)
        case Map(a, b, c) =>
          applyBody(a, b, c)(Map)
        case FlatMap(a, b, c)   => applyBody(a, b, c)(FlatMap)
        case ConcatMap(a, b, c) => applyBody(a, b, c)(ConcatMap)
        case GroupBy(a, b, c)   => applyBody(a, b, c)(GroupBy)
        case SortBy(a, b, c, d) => applyBody(a, b, c)(SortBy(_, _, _, d))
        case Join(t, a, b, iA, iB, on) =>
          val ar = apply(a)
          val br = apply(b)
          val iAr = iA.retypeQuatFrom(ar.quat)
          val iBr = iB.retypeQuatFrom(br.quat)
          val onr = BetaReduction(on, RWR, iA -> iAr, iB -> iBr)
          trace"Repropagate ${a.quat.suppress(msg)} from $a and ${b.quat.suppress(msg)} from $b into:" andReturn Join(t, ar, br, iAr, iBr, apply(onr))
        case FlatJoin(t, a, iA, on) =>
          val ar = apply(a)
          val iAr = iA.retypeQuatFrom(ar.quat)
          val onr = BetaReduction(on, RWR, iA -> iAr)
          trace"Repropagate ${a.quat.suppress(msg)} from $a into:" andReturn FlatJoin(t, a, iAr, apply(onr))
        case other =>
          super.apply(other)
      }

    //println("==================== After ==================")
    //println(io.getquill.util.Messages.qprint(o))
    o
  }

  def reassign(assignments: List[Assignment], quat: Quat) =
    assignments.map {
      case Assignment(alias, property, value) =>
        val aliasR = alias.retypeQuatFrom(quat)
        val propertyR = BetaReduction(property, RWR, alias -> aliasR)
        val valueR = BetaReduction(value, RWR, alias -> aliasR)
        Assignment(aliasR, propertyR, valueR)

      // If the alias is not supposed to be repropagated
      case assignment =>
        assignment
    }

  override def apply(a: Action): Action =
    a match {
      case Insert(q: Query, assignments) =>
        val qr = apply(q)
        val assignmentsR = reassign(assignments, qr.quat)
        trace"Repropagate ${q.quat.suppress(msg)} from $q into:" andReturn
          Insert(qr, assignmentsR)

      case Update(q: Query, assignments) =>
        val qr = apply(q)
        val assignmentsR = reassign(assignments, qr.quat)
        trace"Repropagate ${q.quat.suppress(msg)} from $q into:" andReturn
          Update(qr, assignmentsR)

      case Returning(action: Action, alias, body) =>
        val actionR = apply(action)
        val aliasR = alias.retypeQuatFrom(actionR.quat)
        val bodyR = BetaReduction(body, RWR, alias -> aliasR)
        trace"Repropagate ${alias.quat.suppress(msg)} from $alias into:" andReturn
          Returning(actionR, aliasR, bodyR)

      case ReturningGenerated(action: Action, alias, body) =>
        val actionR = apply(action)
        val aliasR = alias.retypeQuatFrom(actionR.quat)
        val bodyR = BetaReduction(body, RWR, alias -> aliasR)
        trace"Repropagate ${alias.quat.suppress(msg)} from $alias into:" andReturn
          ReturningGenerated(actionR, aliasR, bodyR)

      case oc @ OnConflict(oca: Action, target, act) =>
        val actionR = apply(oca)
        val targetR =
          target match {
            case OnConflict.Properties(props) =>
              val propsR = props.map {
                // Recreate the assignment with new idents but only if we need to repropagate
                case prop @ PropertyMatroshka(ident: Ident, _) =>
                  trace"Repropagate OnConflict.Properties Quat ${oca.quat.suppress(msg)} from $oca into:" andReturn
                    BetaReduction(prop, RWR, ident -> ident.retypeQuatFrom(oca.quat)).asInstanceOf[Property]
                case other =>
                  throw new IllegalArgumentException(s"Malformed onConflict element ${oc}. Could not parse property ${other}")
              }
              OnConflict.Properties(propsR)

            case v @ OnConflict.NoTarget => v
          }
        val actR = act match {
          case OnConflict.Update(assignments) =>
            val assignmentsR =
              assignments.map { assignment =>
                val aliasR = assignment.alias.copy(quat = oca.quat)
                val propertyR = BetaReduction(assignment.property, RWR, assignment.alias -> aliasR)
                val valueR = BetaReduction(assignment.value, RWR, assignment.alias -> aliasR)
                trace"Repropagate OnConflict.Update Quat ${oca.quat.suppress(msg)} from $oca into:" andReturn
                  Assignment(aliasR, propertyR, valueR)
              }
            OnConflict.Update(assignmentsR)
          case _ => act
        }
        trace"Completing OnConflict Repropogation: " andReturn
          OnConflict(actionR, targetR, actR)

      case other => super.apply(other)
    }
}
