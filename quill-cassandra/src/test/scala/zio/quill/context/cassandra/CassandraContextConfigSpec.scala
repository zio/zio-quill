package zio.quill.context.cassandra

import com.typesafe.config.ConfigFactory
import zio.quill.CassandraContextConfig
import zio.quill.base.Spec

class CassandraContextConfigSpec extends Spec {
  "load default preparedStatementCacheSize if not found in configs" in {
    CassandraContextConfig(ConfigFactory.empty()).preparedStatementCacheSize mustBe 1000
  }
}
