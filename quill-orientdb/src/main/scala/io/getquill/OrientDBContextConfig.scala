package io.getquill

import com.typesafe.config.Config

case class OrientDBContextConfig(config: Config) {
  def dbUrl: String = config.getString("dbUrl")
  def username: String = config.getString("username")
  def password: String = config.getString("password")
}