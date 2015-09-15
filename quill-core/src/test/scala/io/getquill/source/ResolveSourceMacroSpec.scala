package io.getquill.source

import io.getquill._
import io.getquill.source.mirror.MirrorSourceTemplate

class ResolveSourceMacroSpec extends Spec {

  "warns if the source can't be resolved at compile time" in {
    object buggySource extends MirrorSourceTemplate
    "buggySource.run(qr1.delete)" must compile
  }

  "warns if the probe fails" in {
    case class Fail()
    "io.getquill.source.mirror.mirrorSource.run(queryable[Fail].delete)" must compile
  }
}
