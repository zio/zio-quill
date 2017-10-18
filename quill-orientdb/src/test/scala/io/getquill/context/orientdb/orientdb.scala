package io.getquill.context.orientdb

import com.orientechnologies.orient.client.remote.OServerAdmin
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy
import io.getquill._
import io.getquill.util.LoadConfig

object orientdb {
  private val conf = OrientDBContextConfig(LoadConfig("ctx"))
  private val databaseName = "GratefulDeadConcerts"
  private val databaseType = "document"
  private val storageMode = "memory"
  private var setupDone = false

  private def setup(): Unit = {
    val existsDatabase = new OServerAdmin(conf.dbUrl)
      .connect(conf.username, conf.password)
      .existsDatabase(databaseName, storageMode)

    if (!existsDatabase) {
      new OServerAdmin(conf.dbUrl)
        .connect(conf.username, conf.password)
        .createDatabase(databaseName, databaseType, storageMode).close()
    }
    val dbConnection = new OPartitionedDatabasePool(s"${conf.dbUrl}/$databaseName", conf.username, conf.password).acquire()
    val schema = dbConnection.getMetadata.getSchema
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
  }

  private def getOrCreateClass(iSchema: OSchemaProxy, iClassName: String): Unit = {
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
