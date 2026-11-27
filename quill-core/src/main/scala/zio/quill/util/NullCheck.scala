package zio.quill.util

object NullCheck {
  def product(v: AnyRef) = v == null || v == None
}
