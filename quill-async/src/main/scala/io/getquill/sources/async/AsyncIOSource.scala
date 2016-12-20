package io.getquill.sources.async

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.RowData
import com.typesafe.scalalogging.Logger

import io.getquill.naming.NamingStrategy
import io.getquill.sources.BindedStatementBuilder
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.sources.sql._

import org.slf4j.LoggerFactory
import scala.util._
import scala.concurrent._

abstract class AsyncIOSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](
  val pool: AsyncPool
) extends SqlSource[D, N, RowData, BindedStatementBuilder[List[Any]]]
  with Decoders
  with Encoders {

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[AsyncIOSource[_, _, _]]))

  trait ActionApply[T] extends Function1[Seq[T], AsyncIO[Seq[Long]]] {
    def apply(param: T): AsyncIO[Long] = apply(List(param)).map(_.head)
  }

  type QueryResult[T] = AsyncIO[Seq[T]]
  type SingleQueryResult[T] = AsyncIO[T]
  type ActionResult[T] = AsyncIO[Long]
  type BatchedActionResult[T] = AsyncIO[Seq[Long]]

  def close(): Unit = pool.close()

  def probe(sql: String): Try[Any] = {
    try {
      val r = Await.result(pool.execute(SqlCmd(sql, identity)), duration.Duration.Inf)
      Success(r)
    } catch {
      case ex: Throwable => Failure(ex)
    }
  }

  def query[T](sql: String, extractor: RowData => T, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]]): AsyncIO[Seq[T]] = {
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    logger.info(expanded.toString)
    QueryCmd(expanded, params(List()), { (r: DBQueryResult) =>
      r.rows match {
        case Some(rs) => rs.map(extractor)
        case None     => List()
      }
    })
  }

  def querySingle[T](sql: String, extractor: RowData => T = identity[RowData] _, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity) = {
    query(sql, extractor, bind).map(handleSingleResult)
  }

  def execute(sql: String, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity, generated: Option[String] = None): AsyncIO[Long] = {
    val (expanded, paramsBuilder) = bind(new SqlBindedStatementBuilder).build(sql)
    val params = paramsBuilder(List())
    logger.info(expanded.toString)
    ExecuteCmd(expanded, params, extractActionResult(generated))
  }

  def executeBatch[T](sql: String, bindParams: T => BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]], generated: Option[String] = None) = new ActionApply[T] {

    def apply(values: Seq[T]): AsyncIO[Seq[Long]] = {
      val empty = AsyncIO.pure(Seq.empty[Long])
      values.foldLeft(empty) { (sumIO, v) =>

        val (expanded, params) = bindParams(v)(new SqlBindedStatementBuilder).build(sql)
        val vIO = ExecuteCmd[Long](expanded, params(List()), extractActionResult(generated))
        logger.info(expanded)
        for {
          s <- sumIO
          v0 <- vIO
        } yield s :+ v0
      }
    }

  }

  protected def extractActionResult(generated: Option[String])(r: DBQueryResult): Long

}
