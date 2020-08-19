package io.getquill

import io.getquill.context.monix.MonixNdbcContext
import monix.execution.Scheduler
import monix.reactive.Observable

trait MonixSpec extends Spec {
  implicit val scheduler = Scheduler.global

  val context: MonixNdbcContext[_, _, _, _] with TestEntities

  def accumulate[T](o: Observable[T]) =
    o.foldLeft(List[T]())({ case (l, elem) => elem +: l })
      .firstL

  def collect[T](o: Observable[T]) =
    accumulate(o).runSyncUnsafe()
}
