package io.getquill.context.jdbc

import com.typesafe.config.ConfigFactory
import io.getquill.JdbcContextConfig
import io.getquill.base.Spec

class JdbcContextConfigSpec extends Spec {
  "fail if cannot load dataSource" in {
    intercept[IllegalStateException] {
      JdbcContextConfig(ConfigFactory.empty()).dataSource
    }
  }
}