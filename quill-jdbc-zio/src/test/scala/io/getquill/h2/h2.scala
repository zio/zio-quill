package io.getquill

package object h2 {
  object testContext extends H2ZioJdbcContext(Literal) with TestEntities
}
