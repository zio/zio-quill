package io.getquill.context

import io.getquill.Literal

import io.getquill.CassandraStreamContext


package object cassandra {

  lazy val testStreamDB = new CassandraStreamContext(Literal, "testStreamDB") with CassandraTestEntities
}
