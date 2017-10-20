package io.getquill.context.cassandra

import com.typesafe.config.ConfigFactory
import io.getquill.{ CassandraContextConfig, Spec }

class CassandraContextConfigSpec extends Spec {
  "load default preparedStatementCacheSize if not found in configs" in {
    CassandraContextConfig(ConfigFactory.empty()).preparedStatementCacheSize mustBe 1000
  }
}
