import io.getquill._

object TestCase {
  def main(args: Array[String]): Unit = {
    val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)

    import ctx._

    case class Person(name: String, age: Int)

    val col = "col"
    def table = "my_table"

    val q1 = quote {
      infix"SELECT #$col FROM #$table".as[Query[Int]]
    }
    //[info] TestCase.scala:18:20: SELECT x.* FROM (SELECT #${col} FROM #${table}) x
    //[info]     println(ctx.run(q1).string)
    //[info] Running TestCase
    //SELECT x.* FROM (SELECT col FROM my_table) x
    println(ctx.run(q1).string)

    implicit class Ops[T](q: Query[T]) {
      def put(v: String) = quote(infix"$q WHERE name = #$v".as[Query[T]])
      //[error] Could not find proxy for v: String in List(value v, method put, class Ops$2, object TestCase, package <empty>, package <root>) (currentOwner= method ast )
      //[error] scala.tools.nsc.transform.LambdaLift$LambdaLifter.scala$tools$nsc$transform$LambdaLift$LambdaLifter$$searchIn$1(LambdaLift.scala:326)
    }

    val name = "some_name"
    val q2 = quote {
      query[Person].put(name)
    }
    //[info] TestCase.scala:34:20: SELECT x.name, x.age FROM person x WHERE name = #${v}
    //[info]     println(ctx.run(q2).string)
    println(ctx.run(q2).string)
  }
}
