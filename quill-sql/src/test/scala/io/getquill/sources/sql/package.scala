package io.getquill.sources

import io.getquill._
import io.getquill.naming.Literal

package object sql {
  val mirrorSource = source(new SqlMirrorSourceConfig[Literal]("test")) 
}

