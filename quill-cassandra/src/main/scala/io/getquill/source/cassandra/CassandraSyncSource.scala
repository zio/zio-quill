package io.getquill.source.cassandra

import scala.collection.JavaConversions.asScalaBuffer
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.naming.NamingStrategy
import com.typesafe.scalalogging.StrictLogging
import scala.annotation.tailrec

class CassandraSyncSource[N <: NamingStrategy]
    extends CassandraSource[N, Row, BoundStatement]
    with CassandraSourceSession
    with Encoders
    with Decoders
    with StrictLogging {

  override type QueryResult[T] = List[T]
  override type ActionResult[T] = ResultSet
  override type BatchedActionResult[T] = List[ResultSet]

  def execute(cql: String): ResultSet = {
    logger.info(cql)
    session.execute(prepare(cql))
  }

  def execute(cql: String, bindList: List[BoundStatement => BoundStatement]): List[ResultSet] = {
    logger.info(cql)
    @tailrec
    def exec(bindList: List[BoundStatement => BoundStatement], acc: List[ResultSet]): List[ResultSet] =
      bindList match {
        case Nil          => List()
        case head :: tail => exec(tail, acc :+ session.execute(prepare(cql, head)))
      }
    exec(bindList, List.empty)
  }

  def query[T](cql: String, bind: BoundStatement => BoundStatement, extractor: Row => T): List[T] = {
    logger.info(cql)
    session.execute(prepare(cql, bind))
      .all.toList.map(extractor)
  }
}
