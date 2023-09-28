package io.getquill

import monix.execution.Scheduler

package object sqlserver {
  Scheduler.global
  object testContext extends SqlServerMonixJdbcContext(Literal, "testSqlServerDB") with TestEntities
}
