package io.getquill

import io.getquill.context.monix.MonixJdbcContext
import io.getquill.context.sql.PeopleSpec
import monix.execution.Scheduler
import monix.reactive.Observable

trait PeopleMonixSpec extends PeopleSpec {

  implicit val scheduler = Scheduler.global

  val context: MonixJdbcContext[_, _]

  import context._

  def collect[T](o: Observable[T]) =
    o.foldLeft(List[T]())({ case (l, elem) => elem +: l })
      .firstL
      .runSyncUnsafe()

  val `Ex 11 query` = quote(query[Person])
  val `Ex 11 expected` = peopleEntries
}
