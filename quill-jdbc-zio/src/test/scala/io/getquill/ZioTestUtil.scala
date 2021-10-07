package io.getquill

import zio.{ RIO, Runtime }
import zio.blocking.Blocking

object ZioTestUtil {
  implicit class RioExt[T](rio: RIO[Blocking, T]) {
    def defaultRun: T = Runtime.default.unsafeRun(rio)
  }
}
