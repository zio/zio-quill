package zio.quill.context.cassandra
import _root_.zio.quill.Literal
import _root_.zio.quill.cassandrazio.Quill
import _root_.zio.quill.context.cassandra.zio.ZioCassandraSpec.runLayerUnsafe

package object zio {
  val pool           = runLayerUnsafe(Quill.CassandraZioSession.fromPrefix("testStreamDB"))
  lazy val testZioDB = new Quill.Cassandra(Literal, pool) with CassandraTestEntities
}
