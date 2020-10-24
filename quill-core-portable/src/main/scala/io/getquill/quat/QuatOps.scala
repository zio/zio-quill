package io.getquill.quat

import io.getquill.ast.Ast
import io.getquill.quotation.QuatException

private[getquill] object QuatOps {
  object Implicits {
    implicit class QuatOpsExt(quat: Quat.Product) {
      def renameAtPath(path: List[String], renames: List[(String, String)]) =
        QuatOps.renameQuatAtPath(path, renames, quat)
    }
  }

  // Grouping renames at particular paths allows us to
  // Apply a set of renames in a particular path to a Quat
  def renameQuatAtPath(path: List[String], renames: List[(String, String)], rootQuat: Quat.Product) = {
    def renameQuatAtPathRecurse(path: List[String], curr: List[String], quat: Quat.Product): Quat.Product =
      path match {
        case Nil =>
          quat.withRenames(renames.filter(r => quat.fields.contains(r._1)))
        case head :: tail =>
          val goInto =
            quat.lookup(head) match {
              case p: Quat.Product => p
              case _ =>
                QuatException(s"Quat at ${curr.mkString("/", ".", "")} is not a product but we need to go into ${tail.mkString("./", ".", "")} and write renames: [${renames.mkString(",")}]")
            }
          val newSubQuat = renameQuatAtPathRecurse(tail, curr :+ head, goInto)
          // Make a copy of the current quat with the one at the recursed field replaced.
          // Note that technically this is an N^2 operation per the number of unique rename paths of entities
          // (Where Nil is the root path, and for every embedded entity, the property alias emb.foo -> bar
          // or emb1.emb2.foo -> bar, Nil, emb, and emb1.emb2 would be unique sub paths.)
          // Technically we could just do "newFields = quat.fields.put(head, newSubQuat) but that would
          // introduce mutability into the Ast that I wish to avoid for now although it should technically
          // be a safe operation because there is no multi-threading in the Quill transformations.
          val newFields = quat.fields.map(kv => if (kv._1 == head) (kv._1, newSubQuat) else kv)
          // Re-create the quat with the new fields. Can't use copy since it would not copy the renames
          // along with the object.
          Quat.Product.WithRenames(quat.tpe, newFields, quat.renames)
      }

    renameQuatAtPathRecurse(path, List(), rootQuat)
  }

  object HasBooleanQuat {
    def unapply(ast: Ast): Option[Ast] =
      if (ast.quat.isInstanceOf[Quat.Boolean]) Some(ast) else None
  }

  object HasBooleanValueQuat {
    def unapply(ast: Ast): Option[Ast] =
      if (ast.quat == Quat.BooleanValue) Some(ast) else None
  }
}
