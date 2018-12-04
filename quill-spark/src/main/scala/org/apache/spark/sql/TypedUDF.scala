package org.apache.spark.sql

import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.expressions.UserDefinedFunction
import scala.reflect.runtime.universe.{ TypeTag, typeTag }
import scala.util.Try
import org.apache.spark.sql.expressions.SparkUserDefinedFunction
import org.apache.spark.sql.types.StructType

object TypedUDF {
  def apply[RT, A1: TypeTag](f: Function1[A1, RT], dataType: StructType): UserDefinedFunction = {
    val inputSchemas = Try(ScalaReflection.schemaFor(typeTag[A1])).toOption :: Nil
    SparkUserDefinedFunction.create(f, dataType, inputSchemas).asNonNullable()
  }
}