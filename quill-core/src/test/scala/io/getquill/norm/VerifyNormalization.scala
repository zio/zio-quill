package io.getquill.norm

import io.getquill.ast._
import io.getquill.util.Messages.fail

object VerifyNormalization extends StatelessTransformer {

  override def apply(q: Query) = verifyFinalFlatMapBody(finalFlatMapBody(q))

  private def finalFlatMapBody(q: Query): Query =
    q match {
      case FlatMap(sourceQuery(a), b, c: FlatMap) => finalFlatMapBody(c)
      case FlatMap(sourceQuery(a), b, c: Query)   => verifyFinalFlatMapBody(c)
      case other                                  => verifyFinalFlatMapBody(q)
    }

  object sourceQuery {
    def unapply(q: Query) =
      q match {
        case _: Entity              => Some(q)
        case _: SortBy | _: Reverse => Some(apply(q))
        case other                  => None
      }
  }

  private def verifyFinalFlatMapBody(q: Query): Query =
    q match {
      case Map(a: Query, b, c) => verifySortByClauses(a)
      case other               => verifySortByClauses(q)
    }

  private def verifySortByClauses(q: Query): Query =
    q match {
      case Reverse(SortBy(a: Query, b, c)) => verifySortByClauses(a)
      case SortBy(a: Query, b, c)          => verifySortByClauses(a)
      case other                           => verifyFilterClause(q)
    }

  private def verifyFilterClause(q: Query): Query =
    q match {
      case q: Filter      => q
      case sourceQuery(q) => q
      case other          => fail(s"Expected 'Filter' or a source query, but got $q")
    }

}
