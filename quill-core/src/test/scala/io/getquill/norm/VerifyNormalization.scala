package io.getquill.norm

import io.getquill.util.Messages._
import io.getquill.ast._

object VerifyNormalization extends StatelessTransformer {

  override def apply(q: Query) = verifyFinalFlatMapBody(finalFlatMapBody(q))

  private def finalFlatMapBody(q: Query): Query =
    q match {
      case FlatMap(a: Entity, b, c: FlatMap) => finalFlatMapBody(c)
      case FlatMap(a: Entity, b, c: Query)   => c
      case other                             => fail(s"Expected a nested or final flatMap but got '$q'")
    }

  private def verifyFinalFlatMapBody(q: Query): Query =
    q match {
      case Map(SortBy(Filter(Entity(a), b, c), d, e), f, g) => q
      case Map(Filter(Entity(a), b, c), d, e) => q
      case Map(SortBy(Entity(a), b, c), d, e) => q
      case SortBy(Filter(Entity(a), b, c), d, e) => q
      case Filter(Entity(a), b, c) => q
      case SortBy(Entity(a), b, c) => q
      case other => fail(s"Not a valid final flatMap body '$q'")
    }

}
