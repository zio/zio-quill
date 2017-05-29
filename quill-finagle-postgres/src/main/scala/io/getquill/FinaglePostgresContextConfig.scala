package io.getquill

import com.twitter.finagle.Postgres
import com.twitter.finagle.Postgres.{ Client, CustomTypes }
import com.twitter.finagle.postgres.PostgresClient
import com.twitter.finagle.postgres.values._
import com.twitter.finagle.service.RetryPolicy
import com.typesafe.config.Config

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
  def customReceiveFunctions: PartialFunction[String, ValueDecoder[T] forSome { type T }] =
    ValueDecoder.decoders orElse { case "noop" => ValueDecoder.unknown }
  def binaryResults: Boolean = Try(config.getBoolean("binaryResults")).getOrElse(false)
  def binaryParams: Boolean = Try(config.getBoolean("binaryParams")).getOrElse(false)

  private def clientSimple: Client = Postgres.Client()
    .withCredentials(username, password)
    .database(database)
    .withCustomReceiveFunctions(customReceiveFunctions)
    .withSessionPool.maxSize(hostConnectionLimit)
    .withBinaryResults(binaryResults)
    .withBinaryParams(binaryParams)
    .withRetryPolicy(RetryPolicy.tries(numRetries))

  private def clientCustomTypes: Client =
    if (!customTypes) clientSimple.withDefaultTypes() else clientSimple.configured(CustomTypes(None))

  private def clientWithSSL: Client = if (useSsl) clientCustomTypes.withTransport.tls else clientCustomTypes

  def client: PostgresClient = clientWithSSL
    .newRichClient(host)
}
