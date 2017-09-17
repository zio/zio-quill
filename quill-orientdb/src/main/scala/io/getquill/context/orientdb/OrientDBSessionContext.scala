package io.getquill.context.orientdb

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.record.impl.ODocument
import io.getquill.NamingStrategy
import io.getquill.context.orientdb.encoding.{ Decoders, Encoders }
import io.getquill.util.Messages.fail

import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import io.getquill.context.Context

abstract class OrientDBSessionContext[N <: NamingStrategy](
  val naming: N,
  dbUrl:      String,
  username:   String,
  password:   String
) extends Context[OrientDBIdiom, N]
  with OrientDBContext[N]
  with Encoders
  with Decoders {

  override type PrepareRow = ArrayBuffer[Any]
  override type ResultRow = ODocument

  override type RunActionReturningResult[T] = Unit
  override type RunBatchActionReturningResult[T] = Unit

  protected val session = new OPartitionedDatabasePool(dbUrl, username, password)
  protected val oDatabase = session.acquire()

  val idiom = OrientDBIdiom

  protected def prepare() = new ArrayBuffer[Any]()

  override def close(): Unit = {
    oDatabase.close()
    session.close()
  }

  override def probe(orientDBQl: String) =
    Try {
      prepare()
      ()
    }

  def executeActionReturning[T](orientDBQl: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor, returningColumn: String): Unit = {
    fail("OrientDB doesn't support `returning`.")
  }

  def executeBatchActionReturning[T](groups: List[BatchGroup], extractor: Extractor[T]): Unit = {
    fail("OrientDB doesn't support `returning`.")
  }
}