package io.getquill

import monix.execution.Scheduler

package object sqlserver {
  private implicit val scheduler = Scheduler.global
  object testContext extends SqlServerMonixJdbcContext(Literal, "testSqlServerDB") with TestEntities
}
