package io.getquill.ast

import scala.reflect.ClassTag

class CollectAst[T](p: PartialFunction[Ast, T], val state: Seq[T])
  extends StatefulTransformer[Seq[T]] {

  override def apply(a: Ast) =
    a match {
      case d if (p.isDefinedAt(d)) => (d, new CollectAst(p, state :+ p(d)))
      case other                   => super.apply(other)
    }
}

object CollectAst {

  def byType[T: ClassTag](a: Ast) =
    apply[T](a) {
      case t: T => t
    }

  def apply[T](a: Ast)(p: PartialFunction[Ast, T]) =
    // The collection is treated as immutable internally but an ArrayBuffer is more effecient then Collection.list at
    // appending which is mostly what the collection does
    (new CollectAst(p, collection.mutable.ArrayBuffer()).apply(a)) match {
      case (_, transformer) =>
        transformer.state
    }
}
