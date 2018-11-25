package io.getquill

import monix.execution.Scheduler

package object h2 {
  private implicit val scheduler = Scheduler.global
  object testContext extends H2MonixJdbcContext(Literal, "testH2DB") with TestEntities
}
