package zio.quill.dsl

import zio.quill.Ord

private[quill] trait OrdDsl {

  implicit def implicitOrd[T]: Ord[T] = Ord.ascNullsFirst
}
