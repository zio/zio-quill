package io.getquill

package object oracle {
  object testContext extends OracleZioJdbcContext(Literal) with TestEntities
}
