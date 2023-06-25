package io.getquill.context.spark.examples

import java.io.File
import java.net.URL
import scala.language.postfixOps
import scala.sys.process._
import org.apache.spark.sql.SparkSession
import io.getquill.Ord
import io.getquill.QuillSparkContext._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case class User(
  id: String,
  login: String,
  gravatar_id: String,
  url: String,
  avatar_url: String
)

case class Repo(
  id: String,
  name: String,
  url: String
)

case class Activity(
  id: String,
  `type`: String,
  actor: User,
  repo: Repo,
  created_at: String,
  org: User
)

object GitHubExample extends App {

  val files =
    for {
      year  <- 2017 to 2017
      month <- 10 to 10
      day   <- 22 to 22
      hour  <- 0 to 23
    } yield "%04d-%02d-%02d-%d".format(year, month, day, hour)

  val f = Future.traverse(files) { name =>
    Future {
      val file = new File(s"$name.json.gz")
      if (!file.exists()) {
        println(s"downloading missing file $file")
        new URL(s"https://data.gharchive.org/$file") #> file !!
      }
    }
  }

  Await.result(f, 30.seconds)

  implicit val sqlContext =
    SparkSession
      .builder()
      .master("local[*]")
      .appName("spark test")
      .getOrCreate()
      .sqlContext

  import sqlContext.implicits._

  val activities = liftQuery(sqlContext.read.json(files.map(n => s"$n.json.gz"): _*).as[Activity])

  val topStargazers = quote {
    activities
      .groupBy(_.actor)
      .map { case (actor, list) =>
        (actor.login, list.size)
      }
      .sortBy { case (login, size) =>
        size
      }(Ord.desc)
  }

  val topProjects = quote {
    activities
      .filter(_.`type` == "WatchEvent")
      .groupBy(_.repo)
      .map { case (repo, list) =>
        (repo.name, list.size)
      }
      .sortBy { case (repoName, size) =>
        size
      }(Ord.desc)
  }

  println(run(topStargazers).show())
  println(run(topProjects).show())
}
