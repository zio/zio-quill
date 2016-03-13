package io.getquill.sources

import io.getquill._
import io.getquill.sources.mirror.MirrorSource

class ResolveSourceMacroSpec extends Spec {

  class BuggyConfig extends MirrorSourceConfig("buggy")

  "fails if the source can't be resolved at compile time" in {
    val s = source(new BuggyConfig with QueryProbing)
    "s.run(qr1)" mustNot compile
  }

  "doesn't warn if query probing is disabled and the source can't be resolved at compile time" in {
    val s = source(new BuggyConfig)
    s.run(qr1.delete)
    ()
  }

  "fails if the probe fails" in {
    case class Fail()
    val s = source(new MirrorSourceConfig("s") with QueryProbing)
    "s.run(query[Fail].delete)" mustNot compile
  }

  "doesn't fail if the quoted source annotation can't be found" in {
    def test(db: MirrorSource) =
      "db.run(qr1.delete)" must compile
  }
}
