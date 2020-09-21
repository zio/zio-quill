package io.getquill.quat

import io.getquill.ast.{ Ast, Ident, Property }

object QuatNestingHelper {
  def valueQuat(quat: Quat): Quat =
    quat match {
      case Quat.BooleanExpression => Quat.BooleanValue
      case Quat.Product(fields)   => Quat.Product(fields.toList.map { case (k, v) => (k, valueQuat(v)) })
      case other                  => other
    }

  def valuefyQuatInProperty(ast: Ast): Ast =
    ast match {
      case Property(id: Ident, name)      => Property(id.copy(quat = valueQuat(id.quat)), name)
      case Property(prop: Property, name) => Property(valuefyQuatInProperty(prop), name)
      case other                          => other
    }
}
