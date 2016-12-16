package io.getquill.sources.async

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.RowData
import com.typesafe.scalalogging.Logger

import io.getquill.naming.NamingStrategy
import io.getquill.sources.BindedStatementBuilder
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.sources.sql._

import org.slf4j.LoggerFactory
import scala.util.Try

class AsyncIOSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](
  config: AsyncIOConfig
) extends SqlSource[D, N, RowData, BindedStatementBuilder[List[Any]]]
  with Decoders
  with Encoders {

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[AsyncIOSource[_, _, _]]))

  trait ActionApply[T] extends Function1[List[T], AsyncIO[List[Long]]] {
    def apply(param: T): AsyncIO[Long] = apply(List(param)).map(_.head)
  }

  type QueryResult[T] = AsyncIO[List[T]]
  type SingleQueryResult[T] = AsyncIO[T]
  type ActionResult[T] = AsyncIO[Long]
  type BatchedActionResult[T] = AsyncIO[List[Long]]

  def close(): Unit = ???

  def probe(sql: String): Try[Any] = ???

  def execute(sql: String, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity, generated: Option[String] = None) = {
    val (expanded, paramsBuilder) = bind(new SqlBindedStatementBuilder).build(sql)
    val params = paramsBuilder(List())
    PrepearedStmtCmd(sql, params)
  }

  def executeBatch[T](sql: String, bindParams: T => BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]], generated: Option[String] = None) = new ActionApply[T] {

    def apply(values: List[T]): AsyncIO[List[Long]] = {
      val empty = AsyncIO.pure(List.empty[Long])
      values.foldLeft(empty) { (sumIO, v) =>
        val (expanded, params) = bindParams(v)(new SqlBindedStatementBuilder).build(sql)
        val vIO = PrepearedStmtCmd[Long](expanded, params(List()))
        for {
          s <- sumIO
          v0 <- vIO
        } yield s :+ v0
      }
    }

  }

}
