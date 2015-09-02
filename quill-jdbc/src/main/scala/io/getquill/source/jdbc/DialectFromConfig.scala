package io.getquill.source.jdbc

import com.typesafe.config.Config
import io.getquill.source.sql.idiom.H2Dialect
import io.getquill.source.sql.idiom.MySQLDialect
import io.getquill.source.sql.idiom.PostgresDialect
import io.getquill.source.sql.idiom.StandardSqlDialect

object DialectFromConfig {

  def apply(config: Config) =
    Option(config.getString("dataSourceClassName")).collect {
      case "org.h2.jdbcx.JdbcDataSource"                   => H2Dialect
      case "org.mariadb.jdbc.MySQLDataSource"              => MySQLDialect
      case "com.mysql.jdbc.jdbc2.optional.MysqlDataSource" => MySQLDialect
      case "com.impossibl.postgres.jdbc.PGDataSource"      => PostgresDialect
      case "org.postgresql.ds.PGSimpleDataSource"          => PostgresDialect
    }.orElse {
      Option(config.getString("jdbcUrl").split(":").toList).collect {
        case jdbc :: "h2" :: url         => H2Dialect
        case jdbc :: "mysql" :: url      => MySQLDialect
        case jdbc :: "postgresql" :: url => PostgresDialect
      }
    }.getOrElse(StandardSqlDialect)
}
