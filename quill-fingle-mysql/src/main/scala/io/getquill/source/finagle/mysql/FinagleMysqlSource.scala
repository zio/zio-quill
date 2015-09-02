package io.getquill.source.finagle.mysql

import java.util.TimeZone
import com.twitter.finagle.exp.mysql.Client
import com.twitter.finagle.exp.mysql.Parameter
import com.twitter.finagle.exp.mysql.Result
import com.twitter.finagle.exp.mysql.Row
import com.twitter.util.Future
import com.twitter.util.Local
import com.typesafe.scalalogging.StrictLogging
import io.getquill.source.sql.SqlSource
import io.getquill.source.sql.idiom.MySQLDialect

class FinagleMysqlSource
    extends SqlSource[Row, List[Parameter]]
    with FinagleMysqlDecoders
    with FinagleMysqlEncoders
    with StrictLogging {

  protected def dateTimezone = TimeZone.getDefault

  protected lazy val client = FinagleMysqlClient(config)

  override lazy val dialect = MySQLDialect

  private val currentClient = new Local[Client]

  def transaction[T](f: => Future[T]) =
    client.transaction {
      transactional =>
        currentClient.update(transactional)
        f.ensure(currentClient.clear)
    }

  def execute(sql: String) =
    withClient(_.prepare(sql)())

  def execute(sql: String, bindList: List[List[Parameter] => List[Parameter]]): Future[List[Result]] =
    bindList match {
      case Nil =>
        Future.value(List())
      case bind :: tail =>
        logger.info(sql)
        withClient(_.prepare(sql)(bind(List()): _*))
          .flatMap(_ => execute(sql, tail))
    }

  def query[T](sql: String, bind: List[Parameter] => List[Parameter], extractor: Row => T) = {
    logger.info(sql)
    withClient(_.prepare(sql).select(bind(List()): _*)(extractor))
  }

  private def withClient[T](f: Client => T) =
    currentClient().map {
      client => f(client)
    }.getOrElse {
      f(client)
    }
}
