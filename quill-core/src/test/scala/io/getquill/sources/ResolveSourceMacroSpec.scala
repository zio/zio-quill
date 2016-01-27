package io.getquill.sources

import io.getquill._

class ResolveSourceMacroSpec extends Spec {

  "warns if the source can't be resolved at compile time" in {
    class Config extends MirrorSourceConfig("buggy")
    source(new Config)
  }

  "doesn't warn if query probing is disabled and the source can't be resolved at compile time" in {
    System.setProperty("disabledSource.queryProbing", "false")
    val s = source(new MirrorSourceConfig("disabledSource"))
    s.run(qr1.delete)
    ()
  }

  "warns if the probe fails" in {
    case class Fail()
    val s = source(new MirrorSourceConfig("s"))
    "s.run(query[Fail].delete)" must compile
  }
}
