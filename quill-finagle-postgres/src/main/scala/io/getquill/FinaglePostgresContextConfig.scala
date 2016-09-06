package io.getquill

import com.typesafe.config.Config
import com.twitter.finagle.postgres.Client
import com.twitter.finagle.postgres.values._
import scala.util.Try

case class FinaglePostgresContextConfig(config: Config) {
  def host: String = config.getString("host")
  def username: String = config.getString("user")
  def password: Option[String] = Try(config.getString("password")).toOption
  def database: String = config.getString("database")
  def useSsl: Boolean = Try(config.getBoolean("useSsl")).getOrElse(false)
  def hostConnectionLimit: Int = Try(config.getInt("hostConnectionLimit")).getOrElse(1)
  def numRetries: Int = Try(config.getInt("numRetries")).getOrElse(4)
  def customTypes: Boolean = Try(config.getBoolean("customTypes")).getOrElse(false)
  def customReceiveFunctions: PartialFunction[String, ValueDecoder[T] forSome { type T }] = { case "noop" => ValueDecoder.Unknown }
  def binaryResults: Boolean = Try(config.getBoolean("binaryResults")).getOrElse(false)
  def binaryParams: Boolean = Try(config.getBoolean("binaryParams")).getOrElse(false)

  def client = Client(
    host = host,
    username = username,
    password = password,
    database = database,
    useSsl = useSsl,
    hostConnectionLimit = hostConnectionLimit,
    numRetries = numRetries,
    customTypes = customTypes,
    customReceiveFunctions = customReceiveFunctions,
    binaryResults = binaryResults,
    binaryParams = binaryParams
  )
}
