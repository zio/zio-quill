package io.getquill.context

import org.apache.spark.sql.SparkSession
import io.getquill.QuillSparkContext

package object spark {

  val sparkSession =
    SparkSession
      .builder()
      .master("local")
      .appName("spark test")
      .getOrCreate()

  implicit val sqlContext = sparkSession.sqlContext

  val testContext = QuillSparkContext
}
