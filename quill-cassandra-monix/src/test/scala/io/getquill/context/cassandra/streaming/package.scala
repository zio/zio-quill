package io.getquill.context.cassandra
import io.getquill.{CassandraStreamContext, Literal}

package object streaming {
  lazy val testStreamDB: CassandraStreamContext[Literal.type] with CassandraTestEntities =
    new CassandraStreamContext(Literal, "testStreamDB") with CassandraTestEntities
}
