package io.getquill

import com.datastax.driver.core._
import io.getquill.CassandraZioContext._
import io.getquill.context.{ ExecutionInfo, StandardContext }
import io.getquill.context.cassandra.{ CassandraRowContext, CqlIdiom }
import io.getquill.context.qzio.ZioContext
import io.getquill.util.Messages.fail
import io.getquill.util.ZioConversions._
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

  protected def page(rs: ResultSet): CIO[Chunk[Row]] = ZIO.succeed {
    val available = rs.getAvailableWithoutFetching
    val builder = ChunkBuilder.make[Row]()
    builder.sizeHint(available)
    while (rs.getAvailableWithoutFetching() > 0) {
      builder += rs.one()
    }
    builder.result()
  }

  private[getquill] def execute(cql: String, prepare: Prepare, csession: CassandraZioSession, fetchSize: Option[Int]) =
    simpleBlocking {
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
          csession.session.executeAsync(p).asZio
        })
    }

  val streamBlocker: ZStream[Any, Nothing, Any] =
    ZStream.managed(
      ZManaged.lock(Blocking.Service.live.blockingExecutor)
    )

  def streamQuery[T](fetchSize: Option[Int], cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext) = {
    val stream =
      for {
        csession <- ZStream.service[CassandraZioSession]
        rs <- ZStream.fromEffect(execute(cql, prepare, csession, fetchSize))
        row <- ZStream.unfoldChunkM(rs) { rs =>
          // keep taking pages while chunk sizes are non-zero
          val nextPage = page(rs)
          nextPage.flatMap { chunk =>
            if (chunk.length > 0) {
              rs.fetchMoreResults().asZio.map(rs => Some((chunk, rs)))
            } else
              ZIO.succeed(None)
          }
        }
      } yield extractor(row, csession)

    // Run the entire chunking flow on the blocking executor
    streamBlocker *> stream
  }

  private[getquill] def simpleBlocking[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    Blocking.Service.live.blocking(zio)

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): CIO[List[T]] = simpleBlocking {
    for {
      csession <- ZIO.service[CassandraZioSession]
      rs <- execute(cql, prepare, csession, None)
      rows <- ZIO.effect(rs.all())
    } yield (rows.asScala.map(row => extractor(row, csession)).toList)
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): CIO[T] = simpleBlocking {
    executeQuery(cql, prepare, extractor)(info, dc).map(handleSingleResult(_))
    for {
      csession <- ZIO.service[CassandraZioSession]
      rs <- execute(cql, prepare, csession, None)
      rows <- ZIO.effect(rs.all())
      singleRow <- ZIO.effect(handleSingleResult(rows.asScala.map(row => extractor(row, csession)).toList))
    } yield singleRow
  }

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext): CIO[Unit] = simpleBlocking {
    for {
      csession <- ZIO.service[CassandraZioSession]
      r <- prepareRowAndLog(cql, prepare).provide(Has(csession))
      result <- csession.session.executeAsync(r).asZio
    } yield ()
  }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): CIO[Unit] = simpleBlocking {
    for {
      env <- ZIO.service[CassandraZioSession]
      result <- {
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
