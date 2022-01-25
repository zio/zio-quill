package io.getquill.context.cassandra.alpakka

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.cassandra.CassandraSessionSettings
import akka.stream.alpakka.cassandra.scaladsl.{ CassandraSessionRegistry, CassandraSession => CassandraAlpakkaSession }
import akka.testkit.TestKit
import io.getquill.context.cassandra.CassandraTestEntities
import io.getquill.{ CassandraAlpakkaContext, Literal, Spec }

import scala.concurrent.ExecutionContext

trait CassandraAlpakkaSpec extends Spec {

  val actorSystem: ActorSystem = ActorSystem("test")
  val alpakkaSession: CassandraAlpakkaSession = CassandraSessionRegistry.get(actorSystem).sessionFor(CassandraSessionSettings())

  implicit val materializer: Materializer = Materializer.matFromSystem(actorSystem)
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val testDB: CassandraAlpakkaContext[Literal.type] with CassandraTestEntities = new CassandraAlpakkaContext(Literal, alpakkaSession, actorSystem.settings.config.getInt("testDB.preparedStatementCacheSize")) with CassandraTestEntities

  override def afterAll(): Unit = {
    alpakkaSession.close(actorSystem.dispatcher)
    TestKit.shutdownActorSystem(actorSystem)
  }

}
