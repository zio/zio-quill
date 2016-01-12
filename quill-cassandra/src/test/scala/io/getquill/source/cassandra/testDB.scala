package io.getquill.source.cassandra

import io.getquill.naming.Literal

object testSyncDB extends CassandraSyncSource[Literal]

object testAsyncDB extends CassandraAsyncSource[Literal]
