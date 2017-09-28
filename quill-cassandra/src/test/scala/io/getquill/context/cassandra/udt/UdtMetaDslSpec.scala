package io.getquill.context.cassandra.udt

import io.getquill.context.cassandra.mirrorContext._

class UdtMetaDslSpec extends UdtSpec {
  "name" in {
    udtMeta[Name]("my_name").name mustBe "my_name"

    // allows dynamic renaming
    val x: String = 123.toString
    udtMeta[Name](x).name mustBe x
  }

  "keyspace" in {
    udtMeta[Name]("ks.name").keyspace mustBe Some("ks")
    udtMeta[Name]("name").keyspace mustBe None
    intercept[IllegalStateException] {
      udtMeta[Name]("ks.name.name")
    }.getMessage mustBe "Cannot parse udt path `ks.name.name`"
  }

  "alias" in {
    val meta = udtMeta[Name]("name", _.lastName -> "last")
    meta.alias("firstName") mustBe None
    meta.alias("lastName") mustBe Some("last")
  }
}
