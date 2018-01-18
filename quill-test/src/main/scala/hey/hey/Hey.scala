package hey.hey

import io.getquill.{ PostgresDialect, SnakeCase, SqlMirrorContext }

object Hey {
  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  case class MyTable(id: Int)

  def main(args: Array[String]): Unit = {

    def runWith(tableName: String) = {
      run {
        querySchemaDynamicName[MyTable](tableName)
      }
    }
    println(runWith("table1").string)
    println(runWith("table2").string)
  }
}