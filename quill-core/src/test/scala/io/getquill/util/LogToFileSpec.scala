package io.getquill.util

import io.getquill.base.Spec

import scala.io.Source

class LogToFileSpec extends Spec {

  // TODO temporarily ignore this test, will release ZIO2 RC1 without query logging support
  "logs a query to file when enabled" ignore {
    val queryLogName = "./LogToFileSpecQuery.sql"
    val mockLogger   = new QueryLogger(LogToFile(queryLogName))

    val mockQuery = "SELECT * from foo_bar where id = ?"

    mockLogger(mockQuery, "io.getquill.util.LogToFileSpec", 15, 5)

    Thread.sleep(1000) // Give the async log a chance to finish up

    val queryFile = Source.fromFile(queryLogName)
    val contents  = queryFile.mkString.trim
    queryFile.close()

    contents must not be empty
    contents must endWith(s"""${mockQuery};""")

  }
}
