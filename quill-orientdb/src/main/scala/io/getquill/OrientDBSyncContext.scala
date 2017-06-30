package io.getquill

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.typesafe.config.Config
import io.getquill.context.orientdb.OrientDBSessionContext
import io.getquill.util.LoadConfig

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

class OrientDBSyncContext[N <: NamingStrategy](
  dbUrl:    String,
  username: String,
  password: String
) extends OrientDBSessionContext[N](dbUrl, username, password) {

  def this(config: OrientDBContextConfig) = this(config.dbUrl, config.username, config.password)
  def this(config: Config) = this(OrientDBContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit

  def executeQuery[T](orientQl: String, prepare: ArrayBuffer[Any] => ArrayBuffer[Any] = identity, extractor: ODocument => T = identity[ODocument] _): List[T] = {
    val objects = prepare(super.prepare()).asJava
    oDatabase.query[java.util.List[ODocument]](new OSQLSynchQuery[ODocument](checkInFilter(orientQl, objects.size())), objects).map(extractor).toList
  }

  def executeQuerySingle[T](orientQl: String, prepare: ArrayBuffer[Any] => ArrayBuffer[Any] = identity, extractor: ODocument => T = identity[ODocument] _): T =
    handleSingleResult(executeQuery(orientQl, prepare, extractor))

  def executeAction[T](orientQl: String, prepare: ArrayBuffer[Any] => ArrayBuffer[Any] = identity): Unit = {
    oDatabase.command(new OCommandSQL(orientQl)).execute(prepare(super.prepare()).toArray)
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