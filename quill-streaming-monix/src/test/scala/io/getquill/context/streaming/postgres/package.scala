package io.getquill.context.streaming

import io.getquill._
import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.util.LoadConfig
import monix.eval.Task
import monix.execution.Scheduler

package object postgres {

  private implicit val scheduler = Scheduler.global
  private val config = StreamingContextConfig(LoadConfig("testPostgresDB"))
  object testContext extends TaskStreamingContext(config.dataSource, PostgresDialect, Literal)
    with TestEntities with TestEncoders with TestDecoders
    with UUIDObjectEncoding[Task]

}
