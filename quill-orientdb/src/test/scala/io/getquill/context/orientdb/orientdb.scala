package io.getquill.context.orientdb

import com.orientechnologies.orient.client.remote.OServerAdmin
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy
import io.getquill._

object orientdb {
  private val dbUrl = "remote:orientdb:2424"
  private val databaseName = "GratefulDeadConcerts"
  private val databaseType = "document"
  private val storageMode = "memory"
  private val username = "root"
  private val password = "root"
  private var setupDone = false

  private def setup(): Unit = {
    val existsDatabase = new OServerAdmin(dbUrl)
      .connect(username, password)
      .existsDatabase(databaseName, storageMode)

    if (!existsDatabase) {
      new OServerAdmin(dbUrl)
        .connect(username, password)
        .createDatabase(databaseName, databaseType, storageMode).close()
    }
    val dbConnection = new OPartitionedDatabasePool(s"$dbUrl/$databaseName", username, password).acquire()
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
    new OrientDBMirrorContext with TestEntities
  }

  def testSyncDB = {
    if (!setupDone) { setup(); setupDone = true }
    new OrientDBSyncContext[Literal](s"$dbUrl/$databaseName", username, password)
  }

  def testAsyncDB = {
    if (!setupDone) { setup(); setupDone = true }
    new OrientDBAsyncContext[Literal](s"$dbUrl/$databaseName", username, password)
  }
}
