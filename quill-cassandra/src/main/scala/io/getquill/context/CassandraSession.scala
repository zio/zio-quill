package io.getquill.context

import com.datastax.oss.driver.api.core.`type`.UserDefinedType
import com.datastax.oss.driver.api.core.data.UdtValue
import com.datastax.oss.driver.api.core.CqlSession
import io.getquill.util.Messages.fail

import scala.jdk.CollectionConverters._
import scala.compat.java8.OptionConverters._

trait CassandraSession extends UdtValueLookup {
  def session: CqlSession
  def preparedStatementCacheSize: Long
  def keyspace: Option[String] = session.getKeyspace.asScala.map(_.toString)

  val udtMetadata: Map[String, List[UserDefinedType]] = session.getMetadata.getKeyspaces.asScala.toList
    .map(_._2)
    .flatMap(_.getUserDefinedTypes.asScala.values)
    .groupBy(_.getName.toString)

  override def udtValueOf(udtName: String, keyspace: Option[String] = None): UdtValue =
    udtMetadata.getOrElse(udtName.toLowerCase, Nil) match {
      case udt :: Nil => udt.newValue()
      case Nil =>
        fail(s"Could not find UDT `$udtName` in any keyspace")
      case udts => udts
        .find(udt => keyspace.contains(udt.getKeyspace.toString) || udt.getKeyspace.toString == session.getKeyspace.get().toString)
        .map(_.newValue())
        .getOrElse(fail(s"Could not determine to which keyspace `$udtName` UDT belongs. " +
          s"Please specify desired keyspace using UdtMeta"))
    }

  def close(): Unit = {
    session.close()
  }
}

trait UdtValueLookup {
  def udtValueOf(udtName: String, keyspace: Option[String] = None): UdtValue = throw new IllegalStateException("UDTs are not supported by this context")
  def session: CqlSession
}
