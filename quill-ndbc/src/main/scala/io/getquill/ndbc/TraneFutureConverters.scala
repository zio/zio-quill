package io.getquill.ndbc

import io.trane.future.scala.{ toJavaFuture, toScalaFuture, Future => TFutureS, Promise => TPromiseS }
import io.trane.future.{ Future => TFutureJ }

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.language.implicitConversions

object TraneFutureConverters {
  implicit def traneScalaToScala[T](tFuture: TFutureS[T]): Future[T] = {
    val promise = Promise[T]()
    tFuture.onComplete(promise.complete)
    promise.future
  }

  implicit def traneJavaToScala[T](jFuture: TFutureJ[T]): Future[T] =
    traneScalaToScala(jFuture.toScala)

  implicit def scalaToTraneScala[T](future: Future[T])(implicit ec: ExecutionContext): TFutureS[T] = {
    val promise = TPromiseS[T]()
    future.onComplete(promise.complete)
    promise.future
  }

  implicit def scalaToTraneJava[T](future: Future[T])(implicit ec: ExecutionContext): TFutureJ[T] =
    scalaToTraneScala(future).toJava

  implicit def traneScalaToTraneJava[T](future: TFutureS[T]): TFutureJ[T] =
    future.toJava

  implicit def traneJavaToTraneScala[T](future: TFutureJ[T]): TFutureS[T] =
    future.toScala
}
