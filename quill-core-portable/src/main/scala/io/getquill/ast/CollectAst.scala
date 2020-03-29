package io.getquill.ast

import scala.reflect.ClassTag

class CollectAst[T](p: PartialFunction[Ast, T], val state: List[T])
  extends StatefulTransformer[List[T]] {

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
    (new CollectAst(p, List()).apply(a)) match {
      case (_, transformer) =>
        transformer.state
    }
}
