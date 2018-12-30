package io.getquill.context.cassandra
import io.getquill.{ CassandraMonixContext, Literal }

package object monix {
  lazy val testMonixDB = new CassandraMonixContext(Literal, "testStreamDB") with CassandraTestEntities
}
