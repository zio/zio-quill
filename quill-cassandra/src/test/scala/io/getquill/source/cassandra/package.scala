package io.getquill.source

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

package object cassandra {

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
