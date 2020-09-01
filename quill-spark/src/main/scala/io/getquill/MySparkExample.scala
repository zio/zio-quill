package io.getquill

import org.apache.spark.sql.SparkSession

case class Person(id: Int, name: String, age: Int)
case class Address(fk: Int, id: Int, street: String)
case class Lot(afk: Int, pfk: Int, name: String)

object MySparkExample {

  import io.getquill.QuillSparkContext._

  val spark =
    SparkSession
      .builder()
      .master("local[*]")
      .appName("spark test")
      .getOrCreate()
  import spark.implicits._

  implicit val sqlContext = spark.sqlContext

  def main(args: Array[String]) = {
    val peopleDS = Seq(Person(1, "Joe", 123)).toDS
    val addressesDS = Seq(Address(1, 11, "123. St")).toDS // if you don't do .toDS: Query not properly normalized. Please open a bug report. Ast: '?'
    val lotsDS = Seq(Lot(1, 11, "123 Place")).toDS

    //val join = quote { liftQuery(people).join(liftQuery(addresses)).on((p, a) => p.id == a.fk) }

    val people = liftQuery(peopleDS)
    val addresses = liftQuery(addressesDS)
    val lots = liftQuery(lotsDS)

    val join = quote {
      for {
        p <- people
        a <- addresses.join(a => p.id == a.fk)
      } yield (p, a)
    }

    val q = quote {
      for {
        l <- lots
        //(p, a) <- join.join { case (p, a) => l.afk == a.id && l.pfk == p.id } // should be possible with joinOn
        (pp, aa) <- join.join(tup => tup match { case (p, a) => l.afk == a.id && l.pfk == p.id })

      } yield (aa.fk, aa, pp.id, l.name)
    }
    run(q).show() //hellooooooooooooooooooooooooooooo

    //run(liftQuery(peopleDS).nested).show()
  }
}

//object RegularQueryExample {
//
//  import io.getquill.MirrorContext
//  val ctx = new MirrorContext(MirrorSqlDialect, Literal)
//  import ctx._
//
//  def main(args: Array[String]) = {
//
//    //val join = quote { liftQuery(people).join(liftQuery(addresses)).on((p, a) => p.id == a.fk) }
//
//    val join = quote {
//      for {
//        p <- query[Person]
//        a <- query[Address].join(a => p.id == a.fk)
//      } yield (p, a)
//    }
//
//    val q = quote {
//      for {
//        l <- query[Lot]
//        //(p, a) <- join.join { case (p, a) => l.afk == a.id && l.pfk == p.id } // should be possible with joinOn
//        (p, a) <- join.join(tup => tup match { case (p, a) => l.afk == a.id && l.pfk == p.id })
//
//      } yield (a.fk, a, p.id, l.name)
//    }
//    println(run(q).string)
//  }
//}
