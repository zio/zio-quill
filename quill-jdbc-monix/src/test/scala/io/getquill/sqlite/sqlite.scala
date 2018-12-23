package io.getquill

import monix.execution.Scheduler

package object sqlite {
  private implicit val scheduler = Scheduler.global
  object testContext extends SqliteMonixJdbcContext(Literal, "testSqliteDB") with TestEntities
}
