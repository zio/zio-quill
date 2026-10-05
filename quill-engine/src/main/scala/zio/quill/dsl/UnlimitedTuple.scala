package zio.quill.dsl

import zio.quill.quotation.NonQuotedException
import scala.annotation.compileTimeOnly

object UnlimitedTuple {
  @compileTimeOnly(NonQuotedException.message)
  def apply(values: Any*): Nothing = NonQuotedException()
}
