package io.getquill

import monix.execution.Scheduler

package object sqlite {
  Scheduler.global
  object testContext extends SqliteMonixJdbcContext(Literal, "testSqliteDB") with TestEntities
}
