package io.getquill.source.finagle.mysql

import com.twitter.finagle.exp.Mysql
import com.typesafe.config.Config

object FinagleMysqlClient {

  def apply(config: Config) = {
    val user = config.getString("user")
    val password = config.getString("password")
    val database = config.getString("database")
    val dest = config.getString("dest")

    Mysql.client
      .withCredentials(user, password)
      .withDatabase(database)
      .newRichClient(dest)
  }
}
