package io.getquill.codegen.integration

import com.typesafe.scalalogging.Logger

import java.io.Closeable
import java.sql.Connection
import javax.sql.DataSource
import io.getquill.codegen.jdbc.DatabaseTypes._
import io.getquill.codegen.jdbc.gen.DefaultJdbcSchemaReader
import io.getquill.codegen.model.JdbcTableMeta
import io.getquill.codegen.util.ConfigPrefix
import io.getquill.codegen.util.OptionOps._
import io.getquill.codegen.util.SchemaConfig
import io.getquill.codegen.util.StringUtil._
import io.getquill.codegen.util.TryOps._
import io.getquill.util.Using.Manager
import org.slf4j.LoggerFactory
import scala.util.Try

object DbHelper {
  private val logger = Logger(LoggerFactory.getLogger(classOf[DbHelper]))

  private def getDatabaseType(ds: DataSource): DatabaseType =
    Manager { use =>
      val conn        = use(ds.getConnection)
      val meta        = conn.getMetaData
      val productType = meta.getDatabaseProductName
      DatabaseType.fromProductName(productType)
    }.flatten.orThrow

  def syncDbRun(rawSql: String, ds: DataSource): Try[Unit] = {
    val databaseType = getDatabaseType(ds)
    val createSchemas =
      """
        |CREATE SCHEMA IF NOT EXISTS Alpha;
        |CREATE SCHEMA IF NOT EXISTS Bravo;
      """.stripMargin

    val mysqlAnsiQuoteMode =
      "SET sql_mode='STRICT_TRANS_TABLES,ANSI_QUOTES';"

    val sql = databaseType match {
      // For mysql need to turn on ansi-quoting mode so that quoted columns will work correctly
      case MySql =>
        List(mysqlAnsiQuoteMode, createSchemas, rawSql).mkString("\n")
      // Oracle does not have built-in 32 or 64 bit integer types so the effect has to be simulated with digits.
      case Oracle =>
        rawSql
          .replaceAll("\\bint\\b", "number(9,0)")
          .replaceAll("\\bbigint\\b", "number(18,0)")
      // Creating schemas is not possible in sqlite
      case Sqlite =>
        rawSql
      case SqlServer =>
        rawSql
          .replaceAll("(?i)Alpha\\.Person", "Alpha.dbo.Person")
          .replaceAll("(?i)Bravo\\.Person", "Bravo.dbo.Person")
      case _ =>
        List(createSchemas, rawSql).mkString("\n")
    }

    if (sql.trim.isEmpty) throw new IllegalArgumentException("Cannot execute empty query")

    val result = Manager { use =>
      appendSequence(use(ds.getConnection), sql.split(";").toList.filter(!_.trim.isEmpty))
    }

    result.map(_ => ())
  }

  private def appendSequence(conn: Connection, actions: List[String]) =
    actions.map { actStr =>
      logger.debug(s"Executing: ${actStr}")
      Manager { use =>
        val stmt = use(conn.prepareStatement(actStr))
        stmt.execute()
      }

    }

  def dropTables(ds: DataSource with Closeable) = {

    val databaseType = getDatabaseType(ds)

    val allTables = databaseType match {
      // For Oracle, need to connect to other schemas to get info
      case Oracle =>
        new DefaultJdbcSchemaReader(databaseType).extractTables(() => ds.getConnection) ++
          new DefaultJdbcSchemaReader(databaseType) { override def schemaPattern(schema: String) = "ALPHA" }
            .extractTables(() => ds.getConnection) ++
          new DefaultJdbcSchemaReader(databaseType) { override def schemaPattern(schema: String) = "BRAVO" }
            .extractTables(() => ds.getConnection)

      // For SQL Server need to run a manual query to get tables from alpha/bravo databases if they exist
      case SqlServer => {
        import io.getquill._
        val ctx = new SqlServerJdbcContext[Literal](Literal, ds)
        import ctx._
        val tables =
          ctx.run(
            sql"""
            (select table_catalog as _1, table_schema as _2, table_name as _3, table_type as _4 from codegen_test.information_schema.tables) UNION
            (select table_catalog as _1, table_schema as _2, table_name as _3, table_type as _4 from alpha.information_schema.tables) UNION
            (select table_catalog as _1, table_schema as _2, table_name as _3, table_type as _4 from bravo.information_schema.tables)
            """.as[Query[(String, String, String, String)]]
          )
        tables.map { case (cat, schema, name, tpe) => JdbcTableMeta(Option(cat), Option(schema), name, Option(tpe)) }
      }

      case _ =>
        new DefaultJdbcSchemaReader(databaseType).extractTables(() => ds.getConnection)
    }

    val getSchema: JdbcTableMeta => Option[String] = databaseType match {
      case MySql     => tm => tm.tableCat
      case SqlServer => tm => tm.tableCat.flatMap(tc => tm.tableSchema.flatMap(ts => Some(s"${tc}.${ts}")))
      case _         => tm => tm.tableSchema
    }

    val tables = allTables.filter { tm =>
      databaseType match {
        case MySql =>
          tm.tableCat.existsInSetNocase("codegen_test", "alpha", "bravo")
        case SqlServer =>
          tm.tableCat.existsInSetNocase("codegen_test", "alpha", "bravo") && tm.tableSchema.exists(
            _.toLowerCase == "dbo"
          )
        case Oracle =>
          tm.tableSchema.existsInSetNocase("codegen_test", "alpha", "bravo")
        case Sqlite => // SQLite does not have individual schemas at all.
          true
        case Postgres =>
          tm.tableSchema.existsInSetNocase("public", "alpha", "bravo")
        case H2 =>
          tm.tableCat.exists(_.toLowerCase == "codegen_test.h2") &&
          tm.tableSchema.exists(_.toLowerCase != "information_schema")
      }
    }

    val query = tables
      .map(t => s"drop table ${getSchema(t).map(_ + ".").getOrElse("") + s""""${t.tableName}""""};")
      .mkString("\n")

    logger.info("Cleanup:\n" + query)

    Option(query).andNotEmpty.foreach(DbHelper.syncDbRun(_, ds).orThrow)
  }
}

class DbHelper(config: SchemaConfig, dbPrefix: ConfigPrefix, ds: DataSource) {
  def setup(): Unit = Option(config.content).andNotEmpty.foreach(setupScript =>
    DbHelper
      .syncDbRun(setupScript, ds)
      .orThrow(
        new IllegalArgumentException(
          s"Database Setup Failed for ${dbPrefix}. Could not execute DB config ${config} command:\n${config.content}",
          _
        )
      )
  )
}
