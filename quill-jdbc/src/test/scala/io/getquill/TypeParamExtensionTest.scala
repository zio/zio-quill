package io.getquill

import io.getquill.context.Context

// Testing we are passing type params explicitly into JdbcContextBase, otherwise
// this file will fail to compile

trait BaseExtensions {
  val context: Context[PostgresDialect, _]
}

trait JDBCExtensions extends BaseExtensions {
  override val context: PostgresJdbcContext[_]
}
