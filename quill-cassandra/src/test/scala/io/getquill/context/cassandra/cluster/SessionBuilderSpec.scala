package io.getquill.context.cassandra.cluster

import com.datastax.oss.driver.api.core.CqlIdentifier

import com.typesafe.config.ConfigFactory
import io.getquill.Spec

import java.util
import scala.jdk.CollectionConverters._
class SessionBuilderSpec extends Spec {

  def envAddress = sys.env.get("CASSANDRA_CONTACT_POINT_0").get

  "creates Builder" - {

    "with a single host" in {
      val cfgString =
        """
          |{
          |   basic.contact-points = [ ${?CASSANDRA_CONTACT_POINT_0} ]
          |   basic.load-balancing-policy.local-datacenter = dc1
          |   basic.config-reload-interval = 7 minutes
          |   basic.request.consistency = LOCAL_QUORUM
          |   advanced.connection.connect-timeout = 21 seconds
          |}
          |""".stripMargin
      val sessionBuilder = SessionBuilder(ConfigFactory.parseString(cfgString).resolve())
      val session = sessionBuilder.build()
      val sessionConfig = session.getContext.getConfig.getDefaultProfile.entrySet().asScala.map(entry => entry.getKey -> entry.getValue).toMap
      session.isClosed must equal(false)
      session.getKeyspace.isPresent must equal(false)
      sessionConfig("basic.load-balancing-policy.local-datacenter") must equal("dc1")
      sessionConfig("basic.contact-points") must equal(new util.ArrayList(List(envAddress).asJava))
      sessionConfig("basic.config-reload-interval") must equal("7 minutes")
      sessionConfig("basic.request.consistency") must equal("LOCAL_QUORUM")
      sessionConfig("advanced.connection.connect-timeout") must equal("21 seconds")
    }

    "with multiple hosts" in {
      val cfgString =
        """
          |{
          |   basic.contact-points = [ ${?CASSANDRA_CONTACT_POINT_0}, ${?CASSANDRA_CONTACT_POINT_1}, "127.0.0.2:29042" ]
          |   basic.load-balancing-policy.local-datacenter = dc1
          |}
          |""".stripMargin
      val sessionBuilder = SessionBuilder(ConfigFactory.parseString(cfgString).resolve())
      val session = sessionBuilder.build()
      val sessionConfig = session.getContext.getConfig.getDefaultProfile.entrySet().asScala.map(entry => entry.getKey -> entry.getValue).toMap
      session.isClosed must equal(false)
      session.getKeyspace.isPresent must equal(false)
      sessionConfig("basic.load-balancing-policy.local-datacenter") must equal("dc1")
      sessionConfig("basic.contact-points") must equal(new util.ArrayList(List(envAddress, "127.0.0.2:29042").asJava))
    }

    "with multiple hosts and keyspace" in {
      val cfgString =
        """
          |{
          |   basic.contact-points = [ ${?CASSANDRA_CONTACT_POINT_0}, "127.0.0.1:9042", "127.0.0.2:9042" ]
          |   basic.load-balancing-policy.local-datacenter = dc1
          |   basic.session-keyspace = "test"
          |}
          |""".stripMargin
      val sessionBuilder = SessionBuilder(ConfigFactory.parseString(cfgString).resolve())
      val session = sessionBuilder.build()
      val sessionConfig = session.getContext.getConfig.getDefaultProfile.entrySet().asScala.map(entry => entry.getKey -> entry.getValue).toMap
      session.isClosed must equal(false)
      session.getKeyspace.get must equal(CqlIdentifier.fromCql("test"))
      sessionConfig("basic.load-balancing-policy.local-datacenter") must equal("dc1")
      sessionConfig("basic.contact-points") must equal(new util.ArrayList(List(envAddress, "127.0.0.1:9042", "127.0.0.2:9042").asJava))
    }

  }
}