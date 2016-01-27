package io.getquill

import io.getquill.sources.SourceConfig
import io.getquill.sources.cassandra.mirror.CassandraMirrorSource

class CassandraMirrorSourceConfig(val name: String) extends SourceConfig[CassandraMirrorSource]
