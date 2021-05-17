package io.getquill.util

import com.google.common.util.concurrent.ListenableFuture
import io.getquill.CassandraZioContext._
import zio.UIO
import zio.interop.guava.fromListenableFuture

object ZioConversions {
  implicit class ZioTaskConverter[A](lf: => ListenableFuture[A]) {
    def asCio: CIO[A] =
      fromListenableFuture(UIO.effectTotal(lf))
  }
}
