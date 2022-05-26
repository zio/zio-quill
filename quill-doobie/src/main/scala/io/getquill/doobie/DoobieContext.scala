package io.getquill.doobie

import io.getquill._
import io.getquill.context.jdbc._

object DoobieContext {

  class H2[N <: NamingStrategy](val naming: N)
    extends DoobieContextBase[H2Dialect, N]
    with H2JdbcContextBase[N]

  class MySQL[N <: NamingStrategy](val naming: N)
    extends DoobieContextBase[MySQLDialect, N]
    with MysqlJdbcContextBase[N]

  class Oracle[N <: NamingStrategy](val naming: N)
    extends DoobieContextBase[OracleDialect, N]
    with OracleJdbcContextBase[N]

  class Postgres[N <: NamingStrategy](val naming: N)
    extends DoobieContextBase[PostgresDialect, N]
    with PostgresJdbcContextBase[N]

  class SQLite[N <: NamingStrategy](val naming: N)
    extends DoobieContextBase[SqliteDialect, N]
    with SqliteJdbcContextBase[N]

  class SQLServer[N <: NamingStrategy](val naming: N)
    extends DoobieContextBase[SQLServerDialect, N]
    with SqlServerJdbcContextBase[N]

}
