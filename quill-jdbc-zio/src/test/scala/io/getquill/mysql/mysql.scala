package io.getquill

package object mysql {
  object testContext extends MysqlZioJdbcContext(Literal) with TestEntities
}
