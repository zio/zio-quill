package io.getquill.context.json

import io.getquill.context.jdbc.{ Decoders, Encoders }

/** Not supproted with Scala 2.11 because zio-json not supported for 2.11 so this is just a stub
 * so that `extends JsonExtensions` can be used in the contexts */
trait PostgresJsonExtensions {
  this: Encoders with Decoders =>

  object jsonb {
  }

}
