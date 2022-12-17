package io.getquill.context.cassandra

import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.{ CassandraPersistenceComponents, CassandraSession }
import com.lightbend.lagom.scaladsl.playjson.{ EmptyJsonSerializerRegistry, JsonSerializerRegistry }
import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomServer }
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

package object utils {

  val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(false).withCluster(true)) { ctx =>
    new LagomApplication(ctx) with AhcWSComponents with CassandraPersistenceComponents with ConfigurationServiceLocatorComponents {

      override def lagomServer: LagomServer = serverFor[DummyService](new DummyService)

      override def jsonSerializerRegistry: JsonSerializerRegistry = EmptyJsonSerializerRegistry
    }
  }

  val cassandraSession: CassandraSession = server.application.cassandraSession
  implicit val executionContext: ExecutionContext = server.application.executionContext
  implicit val materializer: Materializer = server.application.materializer
}
