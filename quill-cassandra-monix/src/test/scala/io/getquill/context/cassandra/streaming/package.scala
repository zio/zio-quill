package io.getquill.context.cassandra
import io.getquill.{ CassandraStreamContext, Literal }

package object streaming {
  lazy val testStreamDB = new CassandraStreamContext(Literal, "testStreamDB") with CassandraTestEntities
}
