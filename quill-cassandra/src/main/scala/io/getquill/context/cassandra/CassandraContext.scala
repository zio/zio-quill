package io.getquill.context.cassandra

import io.getquill.context.Context
import io.getquill.NamingStrategy

trait CassandraContext[N <: NamingStrategy]
  extends Context[CqlIdiom, N]
  with Ops