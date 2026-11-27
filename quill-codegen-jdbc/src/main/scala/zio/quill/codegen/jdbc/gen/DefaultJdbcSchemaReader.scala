package zio.quill.codegen.jdbc.gen

import zio.quill.codegen.jdbc.DatabaseTypes.{DatabaseType, Oracle}
import zio.quill.codegen.jdbc.model.JdbcTypes.{JdbcConnectionMaker, JdbcSchemaReader}
import zio.quill.codegen.model.{JdbcColumnMeta, JdbcTableMeta, RawSchema}
import zio.quill.codegen.util.StringUtil._
import zio.quill.util.Using

import java.sql.{Connection, ResultSet}
import scala.annotation.tailrec
import scala.util.{Failure, Success}

class DefaultJdbcSchemaReader(
  databaseType: DatabaseType
) extends JdbcSchemaReader {

  @tailrec
  private def resultSetExtractor[T](rs: ResultSet, extractor: (ResultSet) => T, acc: List[T] = List.empty): List[T] =
    if (!rs.next())
      acc.reverse
    else
      resultSetExtractor(rs, extractor, extractor(rs) :: acc)

  private[quill] def schemaPattern(schema: String): String =
    databaseType match {
      case Oracle => schema // Oracle meta fetch takes minutes to hours if schema is not specified
      case _      => null
    }

  def jdbcEntityFilter(ts: JdbcTableMeta): Boolean =
    ts.tableType.existsInSetNocase("table", "view", "user table", "user view", "base table")

  private[quill] def extractTables(connectionMaker: () => Connection): List[JdbcTableMeta] = {
    val output = Using.Manager { use =>
      val conn   = use(connectionMaker())
      val schema = conn.getSchema
      val rs     = use {
        conn.getMetaData.getTables(
          null,
          schemaPattern(schema),
          null,
          null
        )
      }
      resultSetExtractor(rs, rs => JdbcTableMeta.fromResultSet(rs))
    }
    val unfilteredJdbcEntities =
      output match {
        case Success(value) => value
        case Failure(e)     => throw e
      }

    unfilteredJdbcEntities.filter(jdbcEntityFilter)
  }

  private[quill] def extractColumns(connectionMaker: () => Connection): List[JdbcColumnMeta] = {
    val output = Using.Manager { use =>
      val conn   = use(connectionMaker())
      val schema = conn.getSchema
      val rs     = use {
        conn.getMetaData.getColumns(
          null,
          schemaPattern(schema),
          null,
          null
        )
      }
      resultSetExtractor(rs, rs => JdbcColumnMeta.fromResultSet(rs))
    }
    output match {
      case Success(value) => value
      case Failure(e)     => throw e
    }
  }

  override def apply(connectionMaker: JdbcConnectionMaker): Seq[RawSchema[JdbcTableMeta, JdbcColumnMeta]] = {
    val tableMap =
      extractTables(connectionMaker)
        .map(t => ((t.tableCat, t.tableSchema, t.tableName), t))
        .toMap

    val columns      = extractColumns(connectionMaker)
    val tableColumns =
      columns
        .groupBy(c => (c.tableCat, c.tableSchema, c.tableName))
        .map { case (tup, cols) => tableMap.get(tup).map(RawSchema(_, cols)) }
        .collect { case Some(tbl) => tbl }

    tableColumns.toSeq
  }
}
