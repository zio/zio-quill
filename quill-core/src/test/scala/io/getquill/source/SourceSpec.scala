package io.getquill.source

import io.getquill.Spec
import io.getquill.source.mirror.mirrorSource

class SourceSpec extends Spec {

  "loads the config" in {
    System.setProperty("mirrorSource.testKey", "testValue")
    mirrorSource.mirrorConfig.getString("testKey") mustEqual "testValue"
  }
}
