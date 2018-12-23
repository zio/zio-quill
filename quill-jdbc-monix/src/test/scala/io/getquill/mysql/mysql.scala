package io.getquill

import monix.execution.Scheduler

package object mysql {
  private implicit val scheduler = Scheduler.global
  object testContext extends MysqlMonixJdbcContext(Literal, "testMysqlDB") with TestEntities
}
