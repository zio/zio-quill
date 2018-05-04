package io.getquill.context

import org.apache.spark.sql.SparkSession
import io.getquill.QuillSparkContext

package object spark {

  val sparkSession =
    SparkSession
      .builder()
      .config("spark.sql.shuffle.partitions", 2) // Default shuffle partitions is 200, too much for tests
      .config("spark.ui.enabled", "false")
      .master("local[1]")
      .appName("spark test")
      .getOrCreate()

  sparkSession.sparkContext.setLogLevel("WARN")

  implicit val sqlContext = sparkSession.sqlContext

  val testContext = QuillSparkContext
}
