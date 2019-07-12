package io.getquill.context.orientdb

import com.orientechnologies.orient.core.db.ODatabasePool
import com.orientechnologies.orient.core.db.ODatabaseType
import com.orientechnologies.orient.core.db.OrientDB
import com.orientechnologies.orient.core.db.OrientDBConfig
import com.orientechnologies.orient.core.metadata.schema.OSchema

import io.getquill.Literal
import io.getquill.OrientDBContextConfig
import io.getquill.OrientDBMirrorContext
import io.getquill.OrientDBSyncContext
import io.getquill.TestEntities
import io.getquill.util.LoadConfig

object orientdb {
  private val databaseName = "GratefulDeadConcerts"
  private var setupDone = false

  private val conf = OrientDBContextConfig(LoadConfig("ctx"))

  private def setup(): Unit = {
    val orientDB = new OrientDB(conf.dbUrl, "root", "root", OrientDBConfig.defaultConfig())
    orientDB.createIfNotExists(databaseName, ODatabaseType.MEMORY)

    val pool = new ODatabasePool(conf.dbUrl, "root", "root")
    val schema = pool.acquire().getMetadata.getSchema
    getOrCreateClass(schema, "DecodeNullTestEntity")
    getOrCreateClass(schema, "EncodingTestEntity")
    getOrCreateClass(schema, "ListEntity")
    getOrCreateClass(schema, "ListsEntity")
    getOrCreateClass(schema, "ListFrozen")
    getOrCreateClass(schema, "MapEntity")
    getOrCreateClass(schema, "MapsEntity")
    getOrCreateClass(schema, "MapFrozen")
    getOrCreateClass(schema, "TestEntity")
    getOrCreateClass(schema, "TestEntity2")
    getOrCreateClass(schema, "TestEntity3")
    getOrCreateClass(schema, "Person")
    getOrCreateClass(schema, "OrderTestEntity")
    getOrCreateClass(schema, "SetsEntity")
    getOrCreateClass(schema, "Contact")
    getOrCreateClass(schema, "Address")
  }

  private def getOrCreateClass(iSchema: OSchema, iClassName: String): Unit = {
    if (!iSchema.existsClass(iClassName)) {
      iSchema.createClass(iClassName)
      ()
    }
  }

  def mirrorContext = {
    if (!setupDone) { setup(); setupDone = true }
    new OrientDBMirrorContext(Literal) with TestEntities
  }

  def testSyncDB = {
    if (!setupDone) { setup(); setupDone = true }
    new OrientDBSyncContext(Literal, "ctx")
  }
}
