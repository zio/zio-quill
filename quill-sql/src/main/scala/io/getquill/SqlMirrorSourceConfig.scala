package io.getquill

import io.getquill.sources.SourceConfig
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.mirror.SqlMirrorSource

class SqlMirrorSourceConfig[N <: NamingStrategy](val name: String)
  extends SourceConfig[SqlMirrorSource[N]]
