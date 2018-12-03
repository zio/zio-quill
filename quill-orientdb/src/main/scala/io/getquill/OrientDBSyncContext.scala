package io.getquill

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.typesafe.config.Config
import io.getquill.context.orientdb.OrientDBSessionContext
import io.getquill.util.{ ContextLogger, LoadConfig }

import scala.collection.JavaConverters._
import io.getquill.monad.SyncIOMonad

class OrientDBSyncContext[N <: NamingStrategy](
  naming:   N,
  dbUrl:    String,
  username: String,
  password: String
) extends OrientDBSessionContext[N](naming, dbUrl, username, password)
  with SyncIOMonad {

  def this(naming: N, config: OrientDBContextConfig) = this(naming: N, config.dbUrl, config.username, config.password)
  def this(naming: N, config: Config) = this(naming: N, OrientDBContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming: N, LoadConfig(configPrefix))

  override type Result[T] = T
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit

  private val logger = ContextLogger(classOf[OrientDBSyncContext[_]])

  override def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] = {
    if (transactional) logger.underlying.warn("OrientDB doesn't support transactions, ignoring `io.transactional`")
    super.performIO(io)
  }

  def executeQuery[T](orientQl: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): List[T] = {
    val (params, objects) = prepare(super.prepare())
    logger.logQuery(orientQl, params)
    oDatabase.query[java.util.List[ODocument]](new OSQLSynchQuery[ODocument](checkInFilter(orientQl, objects.size)), objects.asJava).asScala.map(extractor).toList
  }

  def executeQuerySingle[T](orientQl: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): T =
    handleSingleResult(executeQuery(orientQl, prepare, extractor))

  def executeAction[T](orientQl: String, prepare: Prepare = identityPrepare): Unit = {
    val (params, objects) = prepare(super.prepare())
    logger.logQuery(orientQl, params)
    oDatabase.command(orientQl, objects.asInstanceOf[Seq[Object]]: _*)
    ()
  }

  def executeBatchAction[T](groups: List[BatchGroup]): Unit = {
    groups.foreach {
      case BatchGroup(orientQl, prepare) =>
        prepare.foreach(executeAction(orientQl, _))
    }
  }

  private def checkInFilter(orientQl: String, noOfLifts: Int): String = {
    // Issue with OrientDB IN: https://stackoverflow.com/questions/34391006/orientdb-passing-an-array-to-a-query-using-in-on-an-otype-linklist-field
    val orientInFilterString = s"IN (?)"
    val inFilterString = s"IN (${List.fill(noOfLifts)("?").mkString(", ")})"
    if (orientQl.contains(inFilterString)) {
      orientQl.replace(inFilterString, orientInFilterString)
    } else {
      orientQl
    }
  }
}