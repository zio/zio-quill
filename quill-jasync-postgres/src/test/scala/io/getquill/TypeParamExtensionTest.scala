package io.getquill

import io.getquill.context.Context

// Testing we are passing type params explicitly into AsyncContext, otherwise
// this file will fail to compile

trait BaseExtensions {
  val context: Context[PostgresDialect, _]
}

trait AsyncExtensions extends BaseExtensions {
  override val context: PostgresJAsyncContext[_]
}
