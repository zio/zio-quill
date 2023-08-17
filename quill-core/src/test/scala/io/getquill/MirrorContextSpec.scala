package io.getquill

import io.getquill.base.Spec

class MirrorContextSpec extends Spec {
  val ctx = new MirrorContext(MirrorIdiom, Literal) with TestEntities

  "probe" in {
    ctx.probe("Ok").toOption mustBe defined
    ctx.probe("Fail").toOption mustBe empty
  }

  "close" in {
    ctx.close()
  }
}
