package io.getquill

package object postgres {
  object testContext extends PostgresZioJdbcContext(Literal) with TestEntities
}
