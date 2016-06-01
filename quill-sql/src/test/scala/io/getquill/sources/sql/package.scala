package io.getquill.sources

import io.getquill.naming.Literal
import io.getquill.sources.sql.mirror.SqlMirrorSource

package object sql {
  val mirrorSource = new SqlMirrorSource[Literal]
}

