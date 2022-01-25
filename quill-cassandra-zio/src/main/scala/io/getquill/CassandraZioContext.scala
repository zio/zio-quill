package io.getquill

import com.datastax.oss.driver.api.core.cql.{ AsyncResultSet, BoundStatement, Row }
import io.getquill.CassandraZioContext._
import io.getquill.context.{ ExecutionInfo, StandardContext }
import io.getquill.context.cassandra.{ CassandraRowContext, CqlIdiom }
import io.getquill.context.qzio.ZioContext
import io.getquill.util.Messages.fail
import io.getquill.util.ContextLogger
import zio.stream.ZStream
import zio.{ Chunk, ChunkBuilder, Has, ZIO, ZManaged }
import zio.blocking.Blocking

import scala.jdk.CollectionConverters._
import scala.util.Try

object CassandraZioContext extends CioOps {
  type CIO[T] = ZIO[Has[CassandraZioSession], Throwable, T]
  type CStream[T] = ZStream[Has[CassandraZioSession], Throwable, T]
}

trait CioOps {
  implicit class CioExt[T](cio: CIO[T]) {
    @deprecated("Use provide(Has(session)) instead", "3.7.1")
    def provideSession(session: CassandraZioSession): ZIO[Has[CassandraZioSession], Throwable, T] =
      cio.provide(Has(session))
  }
}

/**
 * Quill context that executes Cassandra queries inside of ZIO. Unlike most other contexts
 * that require passing in a Data Source, this context takes in a `ZioCassandraSession`
 * as a resource dependency which can be provided later (see the `ZioCassandraSession` object for helper methods
 * that assist in doing this).
 *
 * The resource dependency itself is just a Has[ZioCassandraSession]
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
  extends CassandraRowContext[N]
  with ZioContext[CqlIdiom, N]
  with StandardContext[CqlIdiom, N]
  with CioOps {

  private val logger = ContextLogger(classOf[CassandraZioContext[_]])

  override type Error = Throwable
  override type Environment = Has[CassandraZioSession]

  override type StreamResult[T] = CStream[T]
  override type RunActionResult = Unit
  override type Result[T] = CIO[T]

  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunBatchActionResult = Unit

  override type PrepareRow = BoundStatement
  override type ResultRow = Row
  override type Session = CassandraZioSession

  protected def page(rs: AsyncResultSet): CIO[Chunk[Row]] = ZIO.succeed {
    val builder = ChunkBuilder.make[Row](rs.remaining())
    while (rs.remaining() > 0) {
      builder ++= rs.currentPage().asScala
    }
    builder.result()
  }

  private[getquill] def execute(cql: String, prepare: Prepare, csession: CassandraZioSession, fetchSize: Option[Int]) =
    simpleBlocking {
      prepareRowAndLog(cql, prepare)
        .mapEffect { p =>
          fetchSize match {
            case Some(value) => p.setPageSize(value)
            case None        => p
          }
        }
        .flatMap(p => {
          ZIO.fromCompletionStage(csession.session.executeAsync(p))
        })
    }

  val streamBlocker: ZStream[Any, Nothing, Any] =
    ZStream.managed(
      ZManaged.lock(Blocking.Service.live.blockingExecutor)
    )

  def streamQuery[T](fetchSize: Option[Int], cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner) = {
    val stream =
      for {
        csession <- ZStream.service[CassandraZioSession]
        rs <- ZStream.fromEffect(execute(cql, prepare, csession, fetchSize))
        row <- ZStream.unfoldChunkM(rs) { rs =>
          // keep taking pages while chunk sizes are non-zero
          page(rs).flatMap { chunk =>
            (chunk.nonEmpty, rs.hasMorePages) match {
              case (true, true)  => ZIO.fromCompletionStage(rs.fetchNextPage()).map(rs => Some((chunk, rs)))
              case (true, false) => ZIO.some((chunk, rs))
              case (_, _)        => ZIO.none
            }
          }
        }
      } yield extractor(row, csession)

    // Run the entire chunking flow on the blocking executor
    streamBlocker *> stream
  }

  private[getquill] def simpleBlocking[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    Blocking.Service.live.blocking(zio)

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): CIO[List[T]] = simpleBlocking {
    streamQuery[T](None, cql, prepare, extractor)(info, dc).runCollect.map(_.toList)
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): CIO[T] = simpleBlocking {
    for {
      csession <- ZIO.service[CassandraZioSession]
      rs <- execute(cql, prepare, csession, Some(1)) //pull only one record from the DB explicitly.
      rows <- ZIO.effect(rs.currentPage())
      singleRow <- ZIO.effect(handleSingleResult(rows.asScala.map(row => extractor(row, csession)).toList))
    } yield singleRow
  }

  def executeAction(cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner): CIO[Unit] = simpleBlocking {
    for {
      csession <- ZIO.service[CassandraZioSession]
      r <- prepareRowAndLog(cql, prepare).provide(Has(csession))
      _ <- ZIO.fromCompletionStage(csession.session.executeAsync(r))
    } yield ()
  }

  //TODO: Cassandra batch actions applicable to insert/update/delete and  described here:
  //      https://docs.datastax.com/en/dse/6.0/cql/cql/cql_reference/cql_commands/cqlBatch.html
  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): CIO[Unit] = simpleBlocking {
    for {
      env <- ZIO.service[CassandraZioSession]
      _ <- {
        val batchGroups =
          groups.flatMap {
            case BatchGroup(cql, prepare) =>
              prepare
                .map(prep => executeAction(cql, prep)(info, dc).provide(Has(env)))
          }
        ZIO.collectAll(batchGroups)
      }
    } yield ()
  }

  private[getquill] def prepareRowAndLog(cql: String, prepare: Prepare = identityPrepare): CIO[PrepareRow] =
    for {
      env <- ZIO.environment[Has[CassandraZioSession]]
      csession = env.get[CassandraZioSession]
      boundStatement <- {
        ZIO.fromFuture { implicit ec => csession.prepareAsync(cql) }
          .mapEffect(row => prepare(row, csession))
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
