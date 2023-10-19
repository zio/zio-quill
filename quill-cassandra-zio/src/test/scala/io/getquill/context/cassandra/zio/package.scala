package io.getquill.context.cassandra
import io.getquill.Literal
import io.getquill.cassandrazio.Quill
import io.getquill.context.cassandra.zio.ZioCassandraSpec.runLayerUnsafe

package object zio {
  val pool           = runLayerUnsafe(Quill.CassandraZioSession.fromPrefix("testStreamDB"))
  lazy val testZioDB = new Quill.Cassandra(Literal, pool) with CassandraTestEntities
}
