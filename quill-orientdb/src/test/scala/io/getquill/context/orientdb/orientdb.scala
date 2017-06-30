package io.getquill.context.orientdb

import io.getquill._

object orientdb {

  def mirrorContext = new OrientDBMirrorContext with TestEntities

  def testSyncDB = new OrientDBSyncContext[Literal]("remote:127.0.0.1:2424/GratefulDeadConcerts", "root", "root")

  def testAsyncDB = new OrientDBAsyncContext[Literal]("remote:127.0.0.1:2424/GratefulDeadConcerts", "root", "root")
}
