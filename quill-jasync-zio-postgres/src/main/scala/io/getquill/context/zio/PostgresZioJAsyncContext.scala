package io.getquill.context.zio

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.{QueryResult => DBQueryResult}
import io.getquill.ReturnAction.{ReturnColumns, ReturnNothing, ReturnRecord}
import io.getquill.context.zio.jasync.{ArrayDecoders, ArrayEncoders}
import io.getquill.util.Messages.fail
import io.getquill.{NamingStrategy, PostgresDialect, ReturnAction}

import scala.jdk.CollectionConverters._

class PostgresZioJAsyncContext[+N <: NamingStrategy](naming: N)
    extends ZioJAsyncContext[PostgresDialect, N, PostgreSQLConnection](PostgresDialect, naming)
    with ArrayEncoders
    with ArrayDecoders
    with UUIDObjectEncoding {

  override protected def extractActionResult[O](returningAction: ReturnAction, returningExtractor: Extractor[O])(
    result: DBQueryResult
  ): List[O] =
    result.getRows.asScala.toList.map(row => returningExtractor(row, ()))

  override protected def expandAction(sql: String, returningAction: ReturnAction): String =
    returningAction match {
      // The Postgres dialect will create SQL that has a 'RETURNING' clause so we don't have to add one.
      case ReturnRecord => s"$sql"
      // The Postgres dialect will not actually use these below variants but in case we decide to plug
      // in some other dialect into this context...
      case ReturnColumns(columns) => s"$sql RETURNING ${columns.mkString(", ")}"
      case ReturnNothing          => s"$sql"
    }

}
