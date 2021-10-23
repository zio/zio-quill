package io.getquill

package object sqlite {
  object testContext extends SqliteZioJdbcContext(Literal) with TestEntities
}
