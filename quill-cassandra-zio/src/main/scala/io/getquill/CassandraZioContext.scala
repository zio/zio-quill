package io.getquill

import com.datastax.driver.core._
import io.getquill.CassandraZioContext._
import io.getquill.context.StandardContext
import io.getquill.context.cassandra.{ CassandraBaseContext, CqlIdiom }
import io.getquill.context.qzio.ZioContext
import io.getquill.util.Messages.fail
import io.getquill.util.ZioConversions._
import io.getquill.util.ContextLogger
import zio.blocking.{ Blocking, blocking }
import zio.stream.ZStream
import zio.{ Chunk, ChunkBuilder, Has, ZIO, ZManaged }

import scala.jdk.CollectionConverters._
import scala.util.Try

object CassandraZioContext {
  type BlockingSession = Has[CassandraZioSession] with Blocking
  type CIO[T] = ZIO[BlockingSession, Throwable, T]
  type CStream[T] = ZStream[BlockingSession, Throwable, T]
  implicit class CioExt[T](cio: CIO[T]) {
    def provideSession(session: CassandraZioSession): ZIO[Blocking, Throwable, T] =
      for {
        block <- ZIO.environment[Blocking]
        result <- cio.provide(Has(session) ++ block)
      } yield result
  }
}

/**
 * Quill context that executes Cassandra queries inside of ZIO. Unlike most other contexts
 * that require passing in a Data Source, this context takes in a `ZioCassandraSession`
 * as a resource dependency which can be provided later (see the `ZioCassandraSession` object for helper methods
 * that assist in doing this).
 *
 * The resource dependency itself is not just a ZioCassandraSession since the Cassandra API requires blocking in
 * some places (this is the case despite fact that is is asynchronous).
 * Instead it is a `Has[ZioCassandraSession] with Has[Blocking.Service]` which is type-alised as
 * `BlockingSession` hence methods in this context return `ZIO[QConnection, Throwable, T]`.
 * The type `CIO[T]` i.e. Cassandra-IO is an alias for this.
 *
 * Various methods in the `io.getquill.ZioCassandraSession` can assist in simplifying it's creation, for example, you can
 * provide a `Config` object instead of a `ZioCassandraSession` like this
 * (note that the resulting ZioCassandraSession has a closing bracket).
 * {{
 *   val zioSession =
 *     ZioCassandraSession.fromPrefix("testStreamDB")
 * }}
 *
 * If you are using a Plain Scala app however, you will need to manually run it e.g. using zio.Runtime
 * {{
 *   Runtime.default.unsafeRun(MyZioContext.run(query[Person]).provideCustomLayer(zioSession))
 * }}
 */
class CassandraZioContext[N <: NamingStrategy](val naming: N)
  extends CassandraBaseContext[N]
  with ZioContext[CqlIdiom, N]
  with StandardContext[CqlIdiom, N] {

  private val logger = ContextLogger(classOf[CassandraZioContext[_]])

  override type Error = Throwable
  override type Environment = Has[CassandraZioSession] with Blocking

  override type StreamResult[T] = CStream[T]
  override type RunActionResult = Unit
  override type Result[T] = CIO[T]

  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunBatchActionResult = Unit

  override type PrepareRow = BoundStatement
  override type ResultRow = Row

  protected def page(rs: ResultSet): CIO[Chunk[Row]] = ZIO.succeed { // TODO Is this right? Was Task.defer in monix
    val available = rs.getAvailableWithoutFetching
    val builder = ChunkBuilder.make[Row]()
    builder.sizeHint(available)
    while (rs.getAvailableWithoutFetching() > 0) {
      builder += rs.one()
    }
    builder.result()
  }

  private[getquill] def execute(cql: String, prepare: Prepare, csession: CassandraZioSession, fetchSize: Option[Int]) =
    blocking {
      prepareRowAndLog(cql, prepare)
        .mapEffect { p =>
          // Set the fetch size of the result set if it exists
          fetchSize match {
            case Some(value) => p.setFetchSize(value)
            case None        =>
          }
          p
        }
        .flatMap(p => {
          csession.session.executeAsync(p).asCio
        })
    }

  val streamBlocker: ZStream[Blocking, Nothing, Any] =
    ZStream.managed(zio.blocking.blockingExecutor.toManaged_.flatMap { executor =>
      ZManaged.lock(executor)
    })

  def streamQuery[T](fetchSize: Option[Int], cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor) = {
    val stream =
      for {
        env <- ZStream.environment[Has[CassandraZioSession]]
        csession = env.get[CassandraZioSession]
        rs <- ZStream.fromEffect(execute(cql, prepare, csession, fetchSize))
        row <- ZStream.unfoldChunkM(rs) { rs =>
          // keep taking pages while chunk sizes are non-zero
          val nextPage = page(rs)
          nextPage.flatMap { chunk =>
            if (chunk.length > 0) {
              rs.fetchMoreResults().asCio.map(rs => Some((chunk, rs)))
            } else
              ZIO.succeed(None)
          }
        }
      } yield extractor(row)

    // Run the entire chunking flow on the blocking executor
    streamBlocker *> stream
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): CIO[List[T]] = blocking {
    for {
      env <- ZIO.environment[Has[CassandraZioSession] with Blocking]
      csession = env.get[CassandraZioSession]
      rs <- execute(cql, prepare, csession, None)
      rows <- ZIO.effect(rs.all())
    } yield (rows.asScala.map(extractor).toList)
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): CIO[T] = blocking {
    executeQuery(cql, prepare, extractor).map(handleSingleResult(_))
    for {
      env <- ZIO.environment[Has[CassandraZioSession] with Blocking]
      csession = env.get[CassandraZioSession]
      rs <- execute(cql, prepare, csession, None)
      rows <- ZIO.effect(rs.all())
      singleRow <- ZIO.effect(handleSingleResult(rows.asScala.map(extractor).toList))
    } yield singleRow
  }

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare): CIO[Unit] = blocking {
    for {
      env <- ZIO.environment[BlockingSession]
      r <- prepareRowAndLog(cql, prepare).provide(env)
      csession = env.get[CassandraZioSession]
      result <- csession.session.executeAsync(r).asCio
    } yield ()
  }

  def executeBatchAction(groups: List[BatchGroup]): CIO[Unit] = blocking {
    for {
      env <- ZIO.environment[Has[CassandraZioSession] with Blocking]
      result <- {
        val batchGroups =
          groups.flatMap {
            case BatchGroup(cql, prepare) =>
              prepare
                .map(prep => executeAction(cql, prep).provide(env))
          }
        ZIO.collectAll(batchGroups)
      }
    } yield ()
  }

  private[getquill] def prepareRowAndLog(cql: String, prepare: Prepare = identityPrepare): CIO[PrepareRow] =
    for {
      env <- ZIO.environment[Has[CassandraZioSession] with Blocking]
      csession = env.get[CassandraZioSession]
      boundStatement <- {
        csession.prepareAsync(cql)
          .mapEffect(prepare)
          .map(p => p._2)
      }
    } yield boundStatement

  def probingSession: Option[CassandraZioSession] = None

  def probe(statement: String): scala.util.Try[_] = {
    probingSession match {
      case Some(csession) =>
        Try(csession.prepare(statement))
      case None =>
        Try(())
    }
  }

  def close(): Unit = fail("Zio Cassandra Session does not need to be closed because it does not keep internal state.")
}
