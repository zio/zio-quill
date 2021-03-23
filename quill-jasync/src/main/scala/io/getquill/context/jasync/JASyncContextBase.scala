package io.getquill.context.jasync

import com.github.jasync.sql.db.RowData
import io.getquill.NamingStrategy
import io.getquill.context.{Context, TranslateContext}
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom

trait JAsyncContextBase[D <: SqlIdiom, N <: NamingStrategy]
  extends Context[D, N]
  with TranslateContext
  with SqlContext[D, N]
  with Decoders
  with Encoders {

  override type PrepareRow = Seq[Any]
  override type ResultRow = RowData
}
