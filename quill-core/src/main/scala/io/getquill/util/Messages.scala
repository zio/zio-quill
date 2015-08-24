package io.getquill.util

import scala.reflect.macros.whitebox.Context

object Messages {

  def fail(msg: String) =
    throw new IllegalStateException(msg)

  implicit class RichContext(c: Context) {
    def fail(msg: String) =
      c.abort(c.enclosingPosition, msg)

    def warn(msg: String) =
      c.warning(c.enclosingPosition, msg)

    def info(msg: String) =
      c.echo(c.enclosingPosition, msg)

    def debug[T](value: T) = {
      c.echo(c.enclosingPosition, s"$value")
      value
    }
  }
}
