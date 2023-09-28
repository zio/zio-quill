package io.getquill

import monix.execution.Scheduler

package object oracle {
  Scheduler.global
  object testContext extends OracleMonixJdbcContext(Literal, "testOracleDB") with TestEntities
}
