package io.getquill

import monix.execution.Scheduler

package object oracle {
  private implicit val scheduler = Scheduler.global
  object testContext extends OracleMonixJdbcContext(Literal, "testOracleDB") with TestEntities
}
