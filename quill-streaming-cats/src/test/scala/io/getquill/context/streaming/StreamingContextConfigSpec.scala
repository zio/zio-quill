package io.getquill.context.streaming

import com.typesafe.config.ConfigFactory
import io.getquill.{ StreamingContextConfig, Spec }

class StreamingContextConfigSpec extends Spec {
  "fail if cannot load dataSource" in {
    intercept[IllegalStateException] {
      StreamingContextConfig(ConfigFactory.empty()).dataSource
    }
  }
}