package io.getquill.codegen.jdbc.gen

import java.sql.{Connection, ResultSet}

import io.getquill.codegen.jdbc.DatabaseTypes.{DatabaseType, Oracle}
import io.getquill.codegen.jdbc.model.JdbcTypes.{JdbcConnectionMaker, JdbcSchemaReader}
import io.getquill.codegen.model.{JdbcColumnMeta, JdbcTableMeta, RawSchema}
import io.getquill.codegen.util.StringUtil._
import io.getquill.util.Using
import scala.util.{Success, Failure}

import scala.annotation.tailrec
import scala.collection.immutable.List

class DefaultJdbcSchemaReader(
  databaseType: DatabaseType
) extends JdbcSchemaReader {

  @tailrec
  private def resultSetExtractor[T](rs: ResultSet, extractor: (ResultSet) => T, acc: List[T] = List()): List[T] =
    if (!rs.next())
      acc.reverse
    else
      resultSetExtractor(rs, extractor, extractor(rs) :: acc)

  private[getquill] def schemaPattern(schema: String) =
    databaseType match {
      case Oracle => schema // Oracle meta fetch takes minutes to hours if schema is not specified
      case _      => null
    }

  def jdbcEntityFilter(ts: JdbcTableMeta) =
    ts.tableType.existsInSetNocase("table", "view", "user table", "user view", "base table")

  private[getquill] def extractTables(connectionMaker: () => Connection): List[JdbcTableMeta] = {
    val output = Using.Manager { use =>
      val conn   = use(connectionMaker())
      val schema = conn.getSchema
      val rs = use {
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

    unfilteredJdbcEntities.filter(jdbcEntityFilter(_))
  }

  private[getquill] def extractColumns(connectionMaker: () => Connection): List[JdbcColumnMeta] = {
    val output = Using.Manager { use =>
      val conn   = use(connectionMaker())
      val schema = conn.getSchema
      val rs = use {
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

    val columns = extractColumns(connectionMaker)
    val tableColumns =
      columns
        .groupBy(c => (c.tableCat, c.tableSchema, c.tableName))
        .map { case (tup, cols) => tableMap.get(tup).map(RawSchema(_, cols)) }
        .collect { case Some(tbl) => tbl }

    tableColumns.toSeq
  }
}
