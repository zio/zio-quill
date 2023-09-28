package io.getquill
import io.getquill.base.Spec
import io.getquill.context.monix.MonixJdbcContext
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.eval.Task

trait MonixSpec extends Spec {

  implicit val scheduler = Scheduler.global

  val context: MonixJdbcContext[_, _] with TestEntities

  def accumulate[T](o: Observable[T]): Task[List[T]] =
    o.foldLeft(List.empty[T]) { case (l, elem) => elem +: l }.firstL

  def collect[T](o: Observable[T]): List[T] =
    accumulate(o).runSyncUnsafe()
}
