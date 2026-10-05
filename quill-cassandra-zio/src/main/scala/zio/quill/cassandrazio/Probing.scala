package zio.quill.cassandrazio

import zio.quill.CassandraZioSession

import scala.util.Try

trait Probing {
  def probingSession: Option[CassandraZioSession] = None

  def probe(statement: String): scala.util.Try[_] =
    probingSession match {
      case Some(csession) =>
        Try(csession.prepare(statement))
      case None =>
        Try(())
    }
}
