package io.getquill.ast

import scala.collection.immutable.Queue
import scala.reflect.ClassTag

/**
 * The collection is treated as immutable internally but an ArrayBuffer is more
 * efficient then Collection.list at appending which is mostly what the
 * collection does
 */
class CollectAst[T](p: PartialFunction[Ast, T], val state: Queue[T]) extends StatefulTransformer[Queue[T]] {

  override def apply(a: Ast) =
    a match {
      case d if (p.isDefinedAt(d)) => (d, new CollectAst(p, state :+ p(d)))
      case other                   => super.apply(other)
    }
}

object CollectAst {

  def byType[T: ClassTag](a: Ast) =
    apply[T](a) { case t: T =>
      t
    }

  def apply[T](a: Ast)(p: PartialFunction[Ast, T]) =
    (new CollectAst(p, Queue[T]()).apply(a)) match {
      case (_, transformer) =>
        transformer.state
    }
}
