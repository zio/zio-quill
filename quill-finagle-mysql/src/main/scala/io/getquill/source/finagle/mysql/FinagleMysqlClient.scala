package io.getquill.source.finagle.mysql

import com.twitter.finagle.exp.Mysql
import com.twitter.util.Try
import com.typesafe.config.Config


object FinagleMysqlClient {

  def apply(config: Config) = {
    val user = config.getString("user")
    val password = Try(config.getString("password")).getOrElse(null)
    val database = config.getString("database")
    val dest = config.getString("dest")

    Mysql.client
      .withCredentials(user, password)
      .withDatabase(database)
      .newRichClient(dest)
  }
}
