//package test.io.getquill.paper
//
//import io.getquill.Source
//
//object People extends App {
//
//  object db extends Source[List[String]]("testdb") {
//
//    implicit val intEncoder = new Encoder[Int] {
//      def encode(value: Int, index: Int, row: List[String]) =
//        row :+ value.toString
//      def decode(index: Int, row: List[String]) =
//        row(index).toInt
//    }
//
//    implicit val stringEncoder = new Encoder[String] {
//      def encode(value: String, index: Int, row: List[String]) =
//        row :+ value
//      def decode(index: Int, row: List[String]) =
//        row(index)
//    }
//
//    val people = entity[Person]("people")
//    val couples = entity[Couple]("couples")
//  }
//
//  case class Person(name: String, age: Int)
//  case class Couple(her: String, him: String)
//
//  // Example 1
//
//  val differences =
//    for {
//      c <- db.couples
//      w <- db.people
//      m <- db.people if (c.her == w.name && c.him == m.name && w.age > m.age)
//    } yield {
//      (w.name, w.age - m.age)
//    }
//    
//  println(differences)
//
//}