package io.getquill.context.cassandra.cluster

import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import io.getquill.util.Messages._
import com.typesafe.config.Config
import com.datastax.oss.driver.api.core.{ CqlSession, CqlSessionBuilder }
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader

import java.util.function.Supplier

object SessionBuilder {

  /**
   * Reference configuration:
   * https://docs.datastax.com/en/developer/java-driver/4.13/manual/core/configuration/reference/
   * config values should be under datastax-java-driver
   * sample HOCON config section:
   *
   * <pre>{@code  {
   *    basic.contact-points = [ "127.0.0.1:9042" ]
   *    basic.load-balancing-policy.local-datacenter = dc1
   *    basic.config-reload-interval = 7 minutes
   *    basic.request.consistency = LOCAL_QUORUM
   *    advanced.connection.connect-timeout = 21 seconds
   *  }
   * }</pre>
   * @param cfg
   * @return
   */
  def apply(cfg: Config): CqlSessionBuilder = {
    CqlSession.builder()
      .withConfigLoader(
        new DefaultDriverConfigLoader(new Supplier[Config] {
          override def get(): Config = cfg.withFallback(DefaultDriverConfigLoader.DEFAULT_CONFIG_SUPPLIER.get())
        })
      )
  }

}
