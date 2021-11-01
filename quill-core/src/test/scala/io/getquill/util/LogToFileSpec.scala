package io.getquill.util

import io.getquill.testContext._
import io.getquill.util.Messages.LogToFile
import io.getquill.{ Spec, testContext }

import scala.io.Source

class LogToFileSpec extends Spec {

  "logs a query to file when enabled" in {
    val queryLogName = "./LogToFileSpecQuery.sql"
    val mockLogger = new QueryLogger(LogToFile(queryLogName))

    val mockQuery = "SELECT * from foo_bar where id = ?"

    mockLogger.doLog(mockQuery, "io.getquill.util.LogToFileSpec", 15, 5)

    Thread.sleep(1000) // Give the async log a chance to finish up

    val queryFile = Source.fromFile(queryLogName)
    val contents = queryFile.mkString.trim
    queryFile.close()

    contents must not be empty
    contents must endWith(s"""${mockQuery};""")

  }
}
