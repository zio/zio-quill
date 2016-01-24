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
    "io.getquill.source.mirror.mirrorSource.run(query[Fail].delete)" must compile
  }

  "identifies config prefix when source object used directly" in {
    object ObjectSource extends MirrorSourceTemplate
    "ObjectSource.run(qr1.delete)" must compile
    ObjectSource.configPrefix must equal("ObjectSource2")
  }

  "identifies config prefix when source object is assigned to a variable" in {
    object VariableSource extends MirrorSourceTemplate
    val db = VariableSource
    "db.run(qr1.delete)" must compile
    db.configPrefix must equal("VariableSource2")
  }

  "identifies config prefix when source is a class" in {
    class ClassSource() extends MirrorSourceTemplate
    val db = new ClassSource()
    "db.run(qr1.delete)" must compile
    db.configPrefix must equal("ClassSource1")
  }
}
