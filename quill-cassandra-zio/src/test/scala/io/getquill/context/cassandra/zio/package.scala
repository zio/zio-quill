package io.getquill.context.cassandra
import io.getquill.{ CassandraZioContext, Literal }

package object zio {
  lazy val testZioDB = new CassandraZioContext(Literal) with CassandraTestEntities
}

