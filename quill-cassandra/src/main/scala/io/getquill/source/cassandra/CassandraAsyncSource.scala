package io.getquill.source.cassandra

import language.implicitConversions
import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.Future
import scala.concurrent.Promise
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Row
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import io.getquill.naming.NamingStrategy
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.ExecutionContext
import com.datastax.driver.core.ConsistencyLevel
import io.getquill.source.cassandra.encoding.Encoders
import io.getquill.source.cassandra.encoding.Decoders

class CassandraAsyncSource[N <: NamingStrategy]
    extends CassandraSource[N, Row, BoundStatement]
    with CassandraSourceSession
    with Encoders
    with Decoders
    with StrictLogging {

  override type QueryResult[T] = Future[List[T]]
  override type ActionResult[T] = Future[ResultSet]
  override type BatchedActionResult[T] = Future[List[ResultSet]]

  def withConsistencyLevel(level: ConsistencyLevel) =
    new CassandraAsyncSource {
      override protected def config = CassandraAsyncSource.this.config
      override protected def queryConsistencyLevel = Some(level)
      override protected lazy val session = CassandraAsyncSource.this.session
    }

  def execute(cql: String)(implicit ec: ExecutionContext): Future[ResultSet] = {
    logger.info(cql)
    session.executeAsync(prepare(cql))
  }

  def execute(cql: String, bindList: List[BoundStatement => BoundStatement])(implicit ec: ExecutionContext): Future[List[ResultSet]] = {
    logger.info(cql)
    bindList match {
      case Nil => Future.successful(List())
      case head :: tail =>
        session.executeAsync(prepare(cql, head)).flatMap { result =>
          execute(cql, tail).map(result +: _)
        }
    }
  }

  def query[T](cql: String, bind: BoundStatement => BoundStatement, extractor: Row => T)(implicit ec: ExecutionContext): Future[List[T]] = {
    logger.info(cql)
    session.executeAsync(prepare(cql, bind))
      .map(_.all.toList.map(extractor))
  }

  private[this] implicit def toScalaFuture(fut: ResultSetFuture): Future[ResultSet] = {
    val p = Promise[ResultSet]()
    Futures.addCallback(fut,
      new FutureCallback[ResultSet] {
        def onSuccess(r: ResultSet) = {
          p.success(r)
          ()
        }
        def onFailure(t: Throwable) = {
          p.failure(t)
          ()
        }
      })
    p.future
  }
}
