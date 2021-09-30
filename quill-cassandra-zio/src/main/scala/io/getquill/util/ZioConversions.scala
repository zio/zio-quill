package io.getquill.util

import com.google.common.util.concurrent.ListenableFuture
import zio.Task
import zio.interop.guava.fromListenableFuture

object ZioConversions {
  implicit class ZioTaskConverter[A](lf: => ListenableFuture[A]) {
    def asZio: Task[A] =
      fromListenableFuture(_ => lf)
  }
}
