package io.getquill

import java.io.File

import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.SSLConfiguration.Mode
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }

class PostgresJAsyncContextConfigSpec extends Spec {

  "parses ssl config" in {
    val config = ConfigFactory
      .empty()
      .withValue("user", ConfigValueFactory.fromAnyRef("user"))
      .withValue("port", ConfigValueFactory.fromAnyRef(5432))
      .withValue("host", ConfigValueFactory.fromAnyRef("host"))
      .withValue("sslmode", ConfigValueFactory.fromAnyRef("require"))
      .withValue(
        "sslrootcert",
        ConfigValueFactory.fromAnyRef("./server-ca.crt")
      )
      .withValue("sslcert", ConfigValueFactory.fromAnyRef("./client-cert.pem"))
      .withValue("sslkey", ConfigValueFactory.fromAnyRef("./client-key.pk8"))

    val context = new PostgresJAsyncContextConfig(config)
    context.connectionPoolConfiguration.getSsl mustEqual new SSLConfiguration(
      Mode.Require,
      new File("./server-ca.crt"),
      new File("./client-cert.pem"),
      new File("./client-key.pk8")
    )
  }
}
