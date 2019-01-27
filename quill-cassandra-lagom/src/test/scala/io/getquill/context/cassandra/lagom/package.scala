package io.getquill.context.cassandra

import io.getquill.{ CassandraLagomAsyncContext, Literal }
import utils._

package object lagom {
  lazy val testLagomAsyncDB = new CassandraLagomAsyncContext(Literal, cassandraSession) with CassandraTestEntities
}
