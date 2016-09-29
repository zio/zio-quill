package io.getquill

import io.getquill.context.cassandra.CassandraContext
import io.getquill.context.cassandra.CqlIdiom

class CassandraMirrorContextWithQueryProbing extends CassandraMirrorContext with QueryProbing

class CassandraMirrorContext[Naming <: NamingStrategy]
  extends MirrorContext[CqlIdiom, Naming] with CassandraContext[Naming]