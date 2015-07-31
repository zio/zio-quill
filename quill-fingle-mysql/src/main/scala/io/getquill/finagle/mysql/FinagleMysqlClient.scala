package io.getquill.finagle.mysql

import com.typesafe.config.Config
import com.twitter.finagle.exp.Mysql

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