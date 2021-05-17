package io.getquill.context

import com.datastax.driver.core.{ Cluster, UDTValue, UserType }
import io.getquill.util.Messages.fail
import scala.jdk.CollectionConverters._

trait CassandraSession {
  def cluster: Cluster
  def keyspace: String
  def preparedStatementCacheSize: Long

  lazy val session = cluster.connect(keyspace)

  val udtMetadata: Map[String, List[UserType]] = cluster.getMetadata.getKeyspaces.asScala.toList
    .flatMap(_.getUserTypes.asScala)
    .groupBy(_.getTypeName)

  def udtValueOf(udtName: String, keyspace: Option[String] = None): UDTValue =
    udtMetadata.getOrElse(udtName.toLowerCase, Nil) match {
      case udt :: Nil => udt.newValue()
      case Nil =>
        fail(s"Could not find UDT `$udtName` in any keyspace")
      case udts => udts
        .find(udt => keyspace.contains(udt.getKeyspace) || udt.getKeyspace == session.getLoggedKeyspace)
        .map(_.newValue())
        .getOrElse(fail(s"Could not determine to which keyspace `$udtName` UDT belongs. " +
          s"Please specify desired keyspace using UdtMeta"))
    }

  def close(): Unit = {
    session.close()
    cluster.close()
  }
}
