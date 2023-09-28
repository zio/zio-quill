package io.getquill

import monix.execution.Scheduler

package object mysql {
  Scheduler.global
  object testContext extends MysqlMonixJdbcContext(Literal, "testMysqlDB") with TestEntities
}
