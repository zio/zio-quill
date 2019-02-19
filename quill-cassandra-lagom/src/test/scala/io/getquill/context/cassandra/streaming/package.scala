package io.getquill.context.cassandra

import io.getquill.{ CassandraLagomStreamContext, Literal }
import utils._

package object streaming {
  lazy val testStreamDB = new CassandraLagomStreamContext(Literal, cassandraSession) with CassandraTestEntities
}
