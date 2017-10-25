package io.getquill.context.spark

import org.apache.spark.sql.Dataset

sealed trait Binding

case class DatasetBinding[T](ds: Dataset[T]) extends Binding

case class ValueBinding(str: String) extends Binding