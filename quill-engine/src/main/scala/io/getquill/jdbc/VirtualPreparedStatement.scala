package io.getquill.jdbc

import io.getquill.util.Interleave

import java.io.{ InputStream, Reader }
import java.net.URL
import java.sql
import java.sql.{ Blob, Clob, Connection, Date, NClob, ParameterMetaData, PreparedStatement, Ref, ResultSet, ResultSetMetaData, RowId, SQLWarning, SQLXML, Time, Timestamp }
import java.util.Calendar

object VirtualPreparedStatement {
  def failOpsUnsupported(): Nothing =
    throw new IllegalAccessError(
      "Prepared-Statement Operations are not supported by the VirtualPreparedStatement. " +
        "Only variable-setters that can be virtualized."
    )

  def failSetterUnsupported(): Nothing =
    throw new IllegalAccessError(
      "This setter is not supported on the VirtualPreparedStatement."
    )
}

class VirtualPreparedStatement extends PreparedStatement {
  import VirtualPreparedStatement._

  private val data: collection.mutable.SortedMap[Int, String] = collection.mutable.SortedMap()

  private def add(i: Int, value: String) = {
    //println(s"Add index ${i} -> ${value}")
    data += ((i, value))
    ()
  }

  private def checkMonotonic(existingValues: Set[Int]): Either[String, Unit] = {
    val allMissingValues =
      (1 to existingValues.max).foldLeft(List.empty[Int])(
        (missingValues, value) =>
          if (!existingValues.contains(value))
            value +: missingValues
          else
            missingValues
      )
    if (allMissingValues.nonEmpty)
      Left(s"Placeholder variables are missing: ${allMissingValues.reverse}")
    else
      Right(())
  }

  def plugIn(query: String) = {
    for {
      _ <- checkMonotonic(data.keySet.toSet)
      numPlaceholders = query.count(_ == '?')
      _ <- {
        if (numPlaceholders != data.keySet.size)
          Left(s"Number of variables found to substitute ${data.keySet.size} was not the same as the number of `?` placeholders: ${numPlaceholders}")
        else
          Right(())
      }
      interleaved <- {
        val queryParts = query.split('?')
        val interleaved = Interleave(queryParts.toList, data.values.toList)
        val interleavedStr = interleaved.mkString("")
        //println(s"============ INTERLEAVED: ${interleavedStr}")
        Right(interleavedStr)
      }
    } yield interleaved
  }

  override def setInt(i: Int, x: Int): Unit = add(i, x.toString)
  override def setLong(i: Int, x: Long): Unit = add(i, x.toString)
  override def setString(i: Int, x: String): Unit = add(i, DataRenderer.string(x))
  override def setFloat(i: Int, x: Float): Unit = add(i, DataRenderer.float(x))
  override def setDouble(i: Int, x: Double): Unit = add(i, DataRenderer.double(x))
  override def setBoolean(i: Int, x: Boolean): Unit = add(i, DataRenderer.renderBoolean(x))
  override def setNull(i: Int, sqlType: Int): Unit = add(i, "null")
  override def setNull(i: Int, sqlType: Int, typeName: String): Unit = add(i, "null")

  override def setByte(i: Int, x: Byte): Unit = ???
  override def setShort(i: Int, x: Short): Unit = ???
  override def setBigDecimal(i: Int, x: java.math.BigDecimal): Unit = ???
  override def setDate(i: Int, x: Date): Unit = ???
  override def setTime(i: Int, x: Time): Unit = ???
  override def setTimestamp(i: Int, x: Timestamp): Unit = ???
  override def setArray(i: Int, x: sql.Array): Unit = ???
  override def setDate(i: Int, x: Date, cal: Calendar): Unit = ???
  override def setTime(i: Int, x: Time, cal: Calendar): Unit = ???
  override def setTimestamp(i: Int, x: Timestamp, cal: Calendar): Unit = ???

  // Might want to support someday
  override def setObject(i: Int, x: Any, targetSqlType: Int): Unit = failSetterUnsupported()
  override def setObject(i: Int, x: Any): Unit = failSetterUnsupported()
  override def setBytes(i: Int, x: Array[Byte]): Unit = failSetterUnsupported()

  // Probably will never support
  override def setAsciiStream(i: Int, x: InputStream, length: Int): Unit = failSetterUnsupported()
  override def setUnicodeStream(i: Int, x: InputStream, length: Int): Unit = failSetterUnsupported()
  override def setBinaryStream(i: Int, x: InputStream, length: Int): Unit = failSetterUnsupported()
  override def setCharacterStream(i: Int, reader: Reader, length: Int): Unit = failSetterUnsupported()
  override def setRef(i: Int, x: Ref): Unit = failSetterUnsupported()
  override def setBlob(i: Int, x: Blob): Unit = failSetterUnsupported()
  override def setClob(i: Int, x: Clob): Unit = failSetterUnsupported()
  override def setURL(i: Int, x: URL): Unit = failSetterUnsupported()
  override def setRowId(i: Int, x: RowId): Unit = failSetterUnsupported()
  override def setNString(i: Int, value: String): Unit = failSetterUnsupported()
  override def setNCharacterStream(i: Int, value: Reader, length: Long): Unit = failSetterUnsupported()
  override def setNClob(i: Int, value: NClob): Unit = failSetterUnsupported()
  override def setClob(i: Int, reader: Reader, length: Long): Unit = failSetterUnsupported()
  override def setBlob(i: Int, inputStream: InputStream, length: Long): Unit = failSetterUnsupported()
  override def setNClob(i: Int, reader: Reader, length: Long): Unit = failSetterUnsupported()
  override def setSQLXML(i: Int, xmlObject: SQLXML): Unit = failSetterUnsupported()
  override def setObject(i: Int, x: Any, targetSqlType: Int, scaleOrLength: Int): Unit = failSetterUnsupported()
  override def setAsciiStream(i: Int, x: InputStream, length: Long): Unit = failSetterUnsupported()
  override def setBinaryStream(i: Int, x: InputStream, length: Long): Unit = failSetterUnsupported()
  override def setCharacterStream(i: Int, reader: Reader, length: Long): Unit = failSetterUnsupported()
  override def setAsciiStream(i: Int, x: InputStream): Unit = failSetterUnsupported()
  override def setBinaryStream(i: Int, x: InputStream): Unit = failSetterUnsupported()
  override def setCharacterStream(i: Int, reader: Reader): Unit = failSetterUnsupported()
  override def setNCharacterStream(i: Int, value: Reader): Unit = failSetterUnsupported()
  override def setClob(i: Int, reader: Reader): Unit = failSetterUnsupported()
  override def setBlob(i: Int, inputStream: InputStream): Unit = failSetterUnsupported()
  override def setNClob(i: Int, reader: Reader): Unit = failSetterUnsupported()

  override def getMetaData: ResultSetMetaData = failOpsUnsupported()
  override def getParameterMetaData: ParameterMetaData = failOpsUnsupported()
  override def executeQuery(): ResultSet = failOpsUnsupported()
  override def executeUpdate(): Int = failOpsUnsupported()
  override def clearParameters(): Unit = failOpsUnsupported()
  override def execute(): Boolean = failOpsUnsupported()
  override def addBatch(): Unit = failOpsUnsupported()
  override def executeQuery(sql: String): ResultSet = failOpsUnsupported()
  override def executeUpdate(sql: String): Int = failOpsUnsupported()
  override def close(): Unit = failOpsUnsupported()
  override def getMaxFieldSize: Int = failOpsUnsupported()
  override def setMaxFieldSize(max: Int): Unit = failOpsUnsupported()
  override def getMaxRows: Int = failOpsUnsupported()
  override def setMaxRows(max: Int): Unit = failOpsUnsupported()
  override def setEscapeProcessing(enable: Boolean): Unit = failOpsUnsupported()
  override def getQueryTimeout: Int = failOpsUnsupported()
  override def setQueryTimeout(seconds: Int): Unit = failOpsUnsupported()
  override def cancel(): Unit = failOpsUnsupported()
  override def getWarnings: SQLWarning = failOpsUnsupported()
  override def clearWarnings(): Unit = failOpsUnsupported()
  override def setCursorName(name: String): Unit = failOpsUnsupported()
  override def execute(sql: String): Boolean = failOpsUnsupported()
  override def getResultSet: ResultSet = failOpsUnsupported()
  override def getUpdateCount: Int = failOpsUnsupported()
  override def getMoreResults: Boolean = failOpsUnsupported()
  override def setFetchDirection(direction: Int): Unit = failOpsUnsupported()
  override def getFetchDirection: Int = failOpsUnsupported()
  override def setFetchSize(rows: Int): Unit = failOpsUnsupported()
  override def getFetchSize: Int = failOpsUnsupported()
  override def getResultSetConcurrency: Int = failOpsUnsupported()
  override def getResultSetType: Int = failOpsUnsupported()
  override def addBatch(sql: String): Unit = failOpsUnsupported()
  override def clearBatch(): Unit = failOpsUnsupported()
  override def executeBatch(): Array[Int] = failOpsUnsupported()
  override def getConnection: Connection = failOpsUnsupported()
  override def getMoreResults(current: Int): Boolean = failOpsUnsupported()
  override def getGeneratedKeys: ResultSet = failOpsUnsupported()
  override def executeUpdate(sql: String, autoGeneratedKeys: Int): Int = failOpsUnsupported()
  override def executeUpdate(sql: String, columnIndexes: Array[Int]): Int = failOpsUnsupported()
  override def executeUpdate(sql: String, columnNames: Array[String]): Int = failOpsUnsupported()
  override def execute(sql: String, autoGeneratedKeys: Int): Boolean = failOpsUnsupported()
  override def execute(sql: String, columnIndexes: Array[Int]): Boolean = failOpsUnsupported()
  override def execute(sql: String, columnNames: Array[String]): Boolean = failOpsUnsupported()
  override def getResultSetHoldability: Int = failOpsUnsupported()
  override def isClosed: Boolean = failOpsUnsupported()
  override def setPoolable(poolable: Boolean): Unit = failOpsUnsupported()
  override def isPoolable: Boolean = failOpsUnsupported()
  override def closeOnCompletion(): Unit = failOpsUnsupported()
  override def isCloseOnCompletion: Boolean = failOpsUnsupported()
  override def unwrap[T](iface: Class[T]): T = failOpsUnsupported()
  override def isWrapperFor(iface: Class[_]): Boolean = failOpsUnsupported()
}
