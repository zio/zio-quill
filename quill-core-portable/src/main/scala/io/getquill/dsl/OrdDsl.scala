package io.getquill.dsl

import io.getquill.Ord

private[dsl] trait OrdDsl {

  implicit def implicitOrd[T]: Ord[T] = Ord.ascNullsFirst
}
