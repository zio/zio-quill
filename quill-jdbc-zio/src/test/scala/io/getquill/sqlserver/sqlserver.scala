package io.getquill

package object sqlserver {
  object testContext extends SqlServerZioJdbcContext(Literal) with TestEntities
}
