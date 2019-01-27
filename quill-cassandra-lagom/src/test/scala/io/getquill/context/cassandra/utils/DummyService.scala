package io.getquill.context.cassandra.utils

import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service }

class DummyService extends Service {
  override def descriptor: Descriptor = {
    import Service._
    named("dummy")
  }
}
