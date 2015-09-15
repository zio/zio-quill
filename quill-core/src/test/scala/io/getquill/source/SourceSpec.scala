package io.getquill.source

import io.getquill._
import io.getquill.source.mirror.mirrorSource

class SourceSpec extends Spec {

  "loads the config" in {
    mirrorSource.mirrorConfig.getString("testKey") mustEqual "testValue"
  }
}
