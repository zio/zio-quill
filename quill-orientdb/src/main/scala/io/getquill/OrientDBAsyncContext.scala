package io.getquill

import java.util.concurrent.Future

import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.replication.OAsyncReplicationError
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery
import com.typesafe.config.Config
import io.getquill.context.orientdb.OrientDBSessionContext
import io.getquill.util.{ ContextLogger, LoadConfig }
import io.getquill.util.Messages.fail

import scala.collection.JavaConverters._

class OrientDBAsyncContext[N <: NamingStrategy](
  dbUrl:    String,
  username: String,
  password: String
)
  extends OrientDBSessionContext[N](dbUrl, username, password) {

  def this(context: OrientDBContextConfig) = this(context.dbUrl, context.username, context.password)
  def this(config: Config) = this(OrientDBContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override type RunQueryResult[T] = Future[List[T]]
  override type RunQuerySingleResult[T] = Future[T]
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit

  private val logger = ContextLogger(classOf[OrientDBSyncContext[_]])

  def executeQuery[T](orientQl: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[List[T]] = {
    val (params, objects) = prepare(super.prepare())
    logger.logQuery(orientQl, params)
    oDatabase.command(new OSQLNonBlockingQuery[ODocument](
      checkInFilter(orientQl, objects.size),
      new OCommandResultListener {
        var records: List[T] = List()

        override def result(iRecord: scala.Any): Boolean = {
          iRecord match {
            case record: ODocument =>
              records :+= extractor(record)
            case _ =>
              fail("invalid record received")
          }
          true
        }

        override def getResult: AnyRef = records

        override def end(): Unit = ()
      }
    )).execute[Future[List[T]]](objects.asJava)
  }

  def executeQuerySingle[T](orientQl: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[T] = {
    val (params, objects) = prepare(super.prepare())
    logger.logQuery(orientQl, params)
    oDatabase.command(new OSQLNonBlockingQuery[ODocument](
      checkInFilter(orientQl, objects.size),
      new OCommandResultListener {
        var record: T = _

        override def result(iRecord: scala.Any): Boolean = {
          iRecord match {
            case oRecord: ODocument =>
              record = extractor(oRecord)
              false
            case _ =>
              fail("invalid record received")
          }
        }

        override def getResult: Object = record.asInstanceOf[Object]

        override def end(): Unit = ()
      }
    )).execute[Future[T]](objects.asJava)
  }

  def executeAction[T](orientQl: String, prepare: Prepare = identityPrepare): Unit = {
    val (params, objects) = prepare(super.prepare())
    logger.logQuery(orientQl, params)

    oDatabase.command(new OCommandSQL(orientQl).onAsyncReplicationError(new OAsyncReplicationError {
      override def onAsyncReplicationError(iException: Throwable, iRetry: Index): OAsyncReplicationError.ACTION = {
        fail("OrientDB action failed to execute")
      }
    })).execute(objects.toArray)
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