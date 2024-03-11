package io.getquill.context.cassandra.pekko

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.connectors.cassandra.CassandraSessionSettings
import org.apache.pekko.stream.connectors.cassandra.scaladsl.{CassandraSessionRegistry, CassandraSession => CassandraPekkoSession}
import org.apache.pekko.testkit.TestKit
import io.getquill.base.Spec
import io.getquill.context.cassandra.CassandraTestEntities
import io.getquill.{CassandraPekkoContext, Literal}

import scala.concurrent.ExecutionContext

trait CassandraPekkoSpec extends Spec {

  val actorSystem: ActorSystem = ActorSystem("test")
  val pekkoSession: CassandraPekkoSession =
    CassandraSessionRegistry.get(actorSystem).sessionFor(CassandraSessionSettings())

  implicit val materializer: Materializer         = Materializer.matFromSystem(actorSystem)
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val testDB: CassandraPekkoContext[Literal.type] with CassandraTestEntities = new CassandraPekkoContext(
    Literal,
    pekkoSession,
    actorSystem.settings.config.getInt("testDB.preparedStatementCacheSize")
  ) with CassandraTestEntities

  override def afterAll(): Unit = {
    pekkoSession.close(actorSystem.dispatcher)
    TestKit.shutdownActorSystem(actorSystem)
  }

}
