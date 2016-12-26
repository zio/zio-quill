package io.getquill

trait TestExtras extends TestEntities with TestEncoders with TestDecoders {
  this: context.Context[_, _] =>
}

