package io.getquill.context.finagle.mysql

import com.twitter.finagle.mysql.Row
import com.twitter.finagle.mysql.Value

case class SingleValueRow(value: Value) extends Row {
  override val values = IndexedSeq(value)
  override val fields = IndexedSeq.empty
  override def indexOf(columnName: String) = None
}