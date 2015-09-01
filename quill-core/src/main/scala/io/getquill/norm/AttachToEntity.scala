package io.getquill.norm

import scala.reflect.classTag
import io.getquill.util.Messages._
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import scala.reflect.ClassTag

object AttachTo {

  def required[T <: Query: ClassTag](f: (Query, Ident) => Query)(q: Query) =
    apply[T](f)(q).getOrElse {
      fail(s"Can't find a '${classTag[T].runtimeClass.getName}' in $q")
    }

  def apply[T <: Query: ClassTag](f: (Query, Ident) => Query)(q: Query): Option[Query] =
    q match {

      case Map(a: T, b, c)         => Some(Map(f(a, b), b, c))
      case FlatMap(a: T, b, c)     => Some(FlatMap(f(a, b), b, c))
      case Filter(a: T, b, c)      => Some(Filter(f(a, b), b, c))
      case SortBy(a: T, b, c)      => Some(SortBy(f(a, b), b, c))

      case Map(a: Query, b, c)     => apply(f)(a).map(Map(_, b, c))
      case FlatMap(a: Query, b, c) => apply(f)(a).map(FlatMap(_, b, c))
      case Filter(a: Query, b, c)  => apply(f)(a).map(Filter(_, b, c))
      case SortBy(a: Query, b, c)  => apply(f)(a).map(SortBy(_, b, c))

      case e: T                    => Some(f(e, Ident("x")))

      case other                   => None
    }
}
