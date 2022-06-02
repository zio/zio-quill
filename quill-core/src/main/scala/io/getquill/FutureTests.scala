package io.getquill

import io.getquill.context.mirror.{ MirrorSession, Row }
import io.getquill.util.PrintMac

import scala.reflect.ClassTag

//package io.getquill
//
//import io.getquill.util.PrintMac
//
//object MyTest2 {
//  val ctx = new MirrorContext(PostgresDialect, SnakeCase)
//  import ctx._
//
//  case class Person(id: Int, name: String)
//  case class Address(owner: Int, street: String)
//
//  def main(args: Array[String]): Unit = {
//    PrintMac(run {
//      for {
//        p <- query[Person]
//        a <- query[Address].leftJoin(a => a.owner == p.id)
//      } yield (p, a)
//    })
//  }
//}

object FutureTests {
  val ctx = new MirrorContext(PostgresDialect, Literal)
  import ctx._

  //case class Person(id: Int, name: Option[String], age: Int)

  //  object SimpleCase {
  //    case class Person(id: Int, name: String)
  //    case class Address(owner: Int, street: String)
  //
  //    def exec(): Unit = {
  //      PrintMac(run {
  //        for {
  //          p <- query[Address]
  //          a <- query[Person].leftJoin(a => a.id == p.owner)
  //        } yield (p, a)
  //      })
  //    }
  //  }

}