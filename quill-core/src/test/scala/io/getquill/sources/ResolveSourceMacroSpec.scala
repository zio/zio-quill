package io.getquill.sources

import io.getquill._

class ResolveSourceMacroSpec extends Spec {

  class BuggyConfig extends MirrorSourceConfig("buggy")
  
  "warns if the source can't be resolved at compile time" in {
    "source(new BuggyConfig)" must compile
    ()
  }

  "doesn't warn if query probing is disabled and the source can't be resolved at compile time" in {
    val s = source(new BuggyConfig with NoQueryProbing)
    s.run(qr1.delete)
    ()
  }

  "warns if the probe fails" in {
    case class Fail()
    val s = source(new MirrorSourceConfig("s"))
    "s.run(query[Fail].delete)" must compile
  }
}
