package io.getquill.context.spark

import language.postfixOps
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{count => fcount, _}

import io.getquill.QuillSparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Dataset

object TopHashtagsExample extends App {

  implicit val sqlContext =
    SparkSession
      .builder()
      .master("local[*]")
      .appName("spark test")
      .getOrCreate()
      .sqlContext

  case class Tweet(text: String)

  import sqlContext.implicits._

  object rdd {

    def topHashtags(tweets: RDD[Tweet], n: Int): Array[(String, BigInt)] =
      tweets
        .flatMap(_.text.split("\\s+")) // split it into words
        .filter(_.startsWith("#"))     // filter hashtag words
        .map(_.toLowerCase)            // normalize hashtags
        .map((_, BigInt(1)))           // create tuples for counting
        .reduceByKey((a, b) => a + b)  // accumulate counters
        .top(n)(Ordering.by(_._2))     // return ordered top hashtags
  }

  object dataframe {
    def topHashtags(tweets: DataFrame, n: Int): DataFrame =
      tweets
        .select(explode(split($"text", "\\s+"))) // split it into words
        .select(lower($"col") as "word")         // normalize hashtags
        .filter("word like '#%'")                // filter hashtag words
        .groupBy($"word")                        // group by each hashtag
        .agg(fcount("*") as "count")             // aggregate the count
        .orderBy($"count" desc)                  // order
        .limit(n)                                // limit to top results
  }

  object dataset {
    def topHashtags(tweets: Dataset[Tweet], n: Int): Dataset[(String, BigInt)] =
      tweets
        .select($"text".as[String])  // select the text column (Dataframe)
        .flatMap(_.split("\\s+"))    // split it into words    (Dataset)
        .filter(_.startsWith("#"))   // filter hashtag words   (Dataset)
        .map(_.toLowerCase)          // normalize hashtags     (Dataset)
        .groupBy($"value")           // group by each hashtag  (Dataframe)
        .agg(fcount("*") as "count") // aggregate the count    (Dataframe)
        .orderBy($"count" desc)      // order                  (Dataframe)
        .limit(n)                    // limit to top results   (Dataframe)
        .as[(String, BigInt)]        // set the type again     (Dataset)
  }

  object quill {
    def topHashtags(tweets: Dataset[Tweet], n: Int): Dataset[(String, Long)] =
      run {                             // produce a dataset from the Quill query
        liftQuery(tweets)               // trasform the dataset into a Quill query
          .concatMap(_.text.split(" ")) // split into words and unnest results
          .filter(_.startsWith("#"))    // filter hashtag words
          .map(_.toLowerCase)           // normalize hashtags
          .groupBy(word => word)        // group by each hashtag
          .map {                        // map word list to its count
            case (word, list) =>
              (word, list.size)
          }
          .sortBy { // sort by the count desc
            case (word, count) => -count
          }
          .take(lift(n)) // limit to top results
      }
  }

  val tweets = List(Tweet("some #hashTAG #h2"), Tweet("dds #h2 #hashtag #h2 #h3")).toDS()

  val rddR = rdd.topHashtags(tweets.rdd, 10).toList
  val dfR  = dataframe.topHashtags(tweets.toDF(), 10).rdd.toLocalIterator.toList
  val dsR  = dataset.topHashtags(tweets, 10).rdd.toLocalIterator.toList
  val qR   = quill.topHashtags(tweets, 10).rdd.toLocalIterator.toList

  println("rddR: " + rddR)
  println("dfR: " + dfR)
  println("dsR: " + dsR)
  println("qR: " + qR)
}
