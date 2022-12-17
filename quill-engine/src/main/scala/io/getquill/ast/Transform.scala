package io.getquill.ast

class Transform[T](p: PartialFunction[Ast, Ast])
  extends StatelessTransformer {

  override def apply(a: Ast) =
    a match {
      case a if (p.isDefinedAt(a)) => p(a)
      case other                   => super.apply(other)
    }
}

object Transform {
  def apply[T](a: Ast)(p: PartialFunction[Ast, Ast]): Ast =
    new Transform(p).apply(a)
}
