package io.getquill

import language.experimental.macros
import io.getquill.ast.Ident

case class Person(id: Long, name: String, surname: String, age: Int)
case class Address(id: Long, personId: Long, streetId: Long, number: Int)
case class Street(id: Long, name: String, city: String)

object Test extends App {

  object db extends Source[List[String]] {

    implicit val longEncoder = new Encoder[Long] {
      def encode(value: Long, index: Int, row: List[String]) =
        row :+ value.toString
      def decode(index: Int, row: List[String]) =
        row(index).toLong
    }

    implicit val intEncoder = new Encoder[Int] {
      def encode(value: Int, index: Int, row: List[String]) =
        row :+ value.toString
      def decode(index: Int, row: List[String]) =
        row(index).toInt
    }

    implicit val stringEncoder = new Encoder[String] {
      def encode(value: String, index: Int, row: List[String]) =
        row :+ value
      def decode(index: Int, row: List[String]) =
        row(index)
    }

    val person = entity[Person]
    val address = entity[Address]
    val street = entity[Street]

    override def run[T](q: Queryable[T]): Any = macro TestMacro.run[T]
  }

  def q1 = db.person.filter(p => p.name == p.surname).map(p => (p.name, p.age))

  val a: String = q1.run
  
  println(q1)
  println(q1.run)

  def q2 =
    for {
      p <- db.person if (p.name == null)
      a <- db.address if (a.personId == p.id)
    } yield {
      a
    }
  println(q2)
  println(q2.run)

  def q3 =
    for {
      a <- q2
      s <- db.street if (a.streetId == s.id)
    } yield {
      a
    }
  println(q3)
  println(q3.run)

  val byName = Partial {
    (name: String) => db.person.filter(_.name == name)
  }

  val q4 = byName("jesus")

  println(q4)
  println(q4.run)

  val byFullName = Partial {
    (name: String, surname: String) => byName(name).filter(_.surname == surname)
  }

  val q5 = byFullName("flavio", "brasil")

  println(q5)
  println(q5.run)

  val nameEqualsSurname = Partial {
    (p: Person) => p.name == p.surname
  }

  println(nameEqualsSurname)

  val q6 = db.person.filter(nameEqualsSurname(_))

  println(q6)
  println(q6.run)

  val nameIs = Partial {
    (p: Person, name: String) => p.name == name
  }

  val q7 = db.person.filter(nameIs(_, "flavio"))

  println(q7)
  println(q7.run)

  val names = db.person.map(_.name)

  val q8 = for {
    name <- names
    p <- db.person if (p.name == name)
  } yield {
    (p.name, p.age)
  }

  println(q8)
  println(q8.run)

  val q9 = db.address.map(_.personId)

  println(q9)
  println(q9.run)

  val q10 =
    for {
      p1 <- db.person
      p2 <- db.person if (p1.name == p2.name)
    } yield {
      p2
    }

  println(q10)
  println(q10.run)

  val personAndAddress =
    for {
      p <- db.person
      a <- db.address if (a.personId == p.id)
    } yield {
      (p, a)
    }

  val q11 =
    for {
      (pp, aa) <- personAndAddress
      s <- db.street if (aa.streetId == s.id)
    } yield {
      (pp, aa, s.city)
    }

  println(q11)
  println(q11.run)
}
