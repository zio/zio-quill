package io.getquill

import io.getquill.base.Spec
import io.getquill.context.ExecutionType

class BatchActionMultiTest extends Spec {
  // Need to fully type this otherwise scala compiler thinks it's still just 'Context' from the super-class
  // and the extensions (m: MirrorContext[_, _]#BatchActionMirror) etc... classes in Spec don't match their types correctly
  val ctx: MirrorContext[PostgresDialect, Literal] = new MirrorContext[PostgresDialect, Literal](PostgresDialect, Literal)
  import ctx._

  case class Person(id: Int, name: String, age: Int)
  val insertPeople = quote((p: Person) => query[Person].insertValue(p))
  val insertPeopleDynamic: Quoted[Person => Insert[Person]] = quote((p: Person) => query[Person].insertValue(p))

  val updatePeopleById = quote((p: Person) => query[Person].filter(pt => pt.id == p.id).updateValue(p))
  val updatePeopleByIdDynamic: Quoted[Person => Update[Person]] = quote((p: Person) => query[Person].filter(pt => pt.id == p.id).updateValue(p))

  "Multi-row Batch Action Should work with" - {
    "inserts > batch-size - (2rows + 2rows) + (1row)" - {
      val people = List(Person(1, "A", 111), Person(2, "B", 222), Person(3, "C", 333), Person(4, "D", 444), Person(5, "E", 555))
      def expect(executionType: ExecutionType) =
        List(
          (
            "INSERT INTO Person (id,name,age) VALUES (?, ?, ?), (?, ?, ?)",
            List(List(1, "A", 111, 2, "B", 222), List(3, "C", 333, 4, "D", 444)),
            executionType
          ),
          (
            "INSERT INTO Person (id,name,age) VALUES (?, ?, ?)",
            List(List(5, "E", 555)),
            executionType
          )
        )

      "static" in {
        val static = ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2)
        static.tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
      }
    }

    "batch insert - (2rows + 2rows)" - {
      val people = List(Person(1, "A", 111), Person(2, "B", 222), Person(3, "C", 333), Person(4, "D", 444))
      def expect(executionType: ExecutionType) =
        List(
          (
            "INSERT INTO Person (id,name,age) VALUES (?, ?, ?), (?, ?, ?)",
            List(List(1, "A", 111, 2, "B", 222), List(3, "C", 333, 4, "D", 444)),
            executionType
          )
        )

      "static" in {
        val static = ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2)
        static.tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
      }
    }

    "inserts == batch-size" - {
      val people = List(Person(1, "A", 111), Person(2, "B", 222))
      def expect(executionType: ExecutionType) =
        List(
          (
            "INSERT INTO Person (id,name,age) VALUES (?, ?, ?), (?, ?, ?)",
            List(List(1, "A", 111, 2, "B", 222)),
            executionType
          )
        )

      "static" in {
        val static = ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2)
        static.tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
      }
    }

    "inserts < batch-size - (1row)" - {
      val people = List(Person(1, "A", 111))
      def expect(executionType: ExecutionType) =
        List(
          (
            "INSERT INTO Person (id,name,age) VALUES (?, ?, ?)",
            List(List(1, "A", 111)),
            executionType
          )
        )

      "static" in {
        val static = ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2)
        static.tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
      }
    }

    "fallback for non-insert query (in a context that doesn't support update)" - {
      val ctx: MirrorContext[MySQLDialect, Literal] = new MirrorContext[MySQLDialect, Literal](MySQLDialect, Literal)
      import ctx._
      val people = List(Person(1, "A", 111), Person(2, "B", 222), Person(3, "C", 333), Person(4, "D", 444), Person(5, "E", 555))
      def expect(executionType: ExecutionType) =
        List(
          (
            "UPDATE Person pt SET id = ?, name = ?, age = ? WHERE pt.id = ?",
            List(List(1, "A", 111, 1), List(2, "B", 222, 2), List(3, "C", 333, 3), List(4, "D", 444, 4), List(5, "E", 555, 5)),
            executionType
          )
        )

      "static" in {
        val static = ctx.run(quote(liftQuery(people).foreach(p => updatePeopleById(p))), 2)
        static.tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
      }
    }

    "update query" - {
      val people = List(Person(1, "A", 111), Person(2, "B", 222), Person(3, "C", 333), Person(4, "D", 444), Person(5, "E", 555))
      def expect(executionType: ExecutionType) =
        List(
          (
            "UPDATE Person AS pt SET id = p.id1, name = p.name, age = p.age FROM (VALUES (?, ?, ?, ?), (?, ?, ?, ?)) AS p(id, id1, name, age) WHERE pt.id = p.id",
            List(List(1, 1, "A", 111, 2, 2, "B", 222), List(3, 3, "C", 333, 4, 4, "D", 444)),
            executionType
          ), (
            "UPDATE Person AS pt SET id = p.id1, name = p.name, age = p.age FROM (VALUES (?, ?, ?, ?)) AS p(id, id1, name, age) WHERE pt.id = p.id",
            List(List(5, 5, "E", 555)),
            executionType
          )
        )

      "static" in {
        val static = ctx.run(quote(liftQuery(people).foreach(p => updatePeopleById(p))), 2)
        static.tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
      }
    }

    "supported contexts" - {
      val people = List(Person(1, "A", 111), Person(2, "B", 222), Person(3, "C", 333), Person(4, "D", 444), Person(5, "E", 555))
      def makeRow(executionType: ExecutionType)(queryA: String, queryB: String) =
        List(
          (
            queryA,
            List(List(1, "A", 111, 2, "B", 222), List(3, "C", 333, 4, "D", 444)),
            executionType
          ),
          (
            queryB,
            List(List(5, "E", 555)),
            executionType
          )
        )

      def makeRowNoIds(executionType: ExecutionType)(queryA: String, queryB: String) =
        List(
          (
            queryA,
            List(List("A", 111, "B", 222), List("C", 333, "D", 444)),
            executionType
          ),
          (
            queryB,
            List(List("E", 555)),
            executionType
          )
        )

      def expect(executionType: ExecutionType) =
        makeRow(executionType)(
          "INSERT INTO Person (id,name,age) VALUES (?, ?, ?), (?, ?, ?)",
          "INSERT INTO Person (id,name,age) VALUES (?, ?, ?)"
        )

      def expectH2(executionType: ExecutionType) =
        makeRow(executionType)(
          "INSERT INTO Person (id,name,age) VALUES ($1, $2, $3), ($4, $5, $6)",
          "INSERT INTO Person (id,name,age) VALUES ($1, $2, $3)"
        )

      def expectH2Returning(executionType: ExecutionType) =
        makeRowNoIds(executionType)(
          "INSERT INTO Person (name,age) VALUES ($1, $2), ($3, $4)",
          "INSERT INTO Person (name,age) VALUES ($1, $2)"
        )

      def expectPostgresReturning(executionType: ExecutionType) =
        makeRow(executionType)(
          "INSERT INTO Person (id,name,age) VALUES (?, ?, ?), (?, ?, ?) RETURNING id", //
          "INSERT INTO Person (id,name,age) VALUES (?, ?, ?) RETURNING id"
        )

      def expectSqlServerReturning(executionType: ExecutionType) =
        makeRow(executionType)(
          "INSERT INTO Person (id,name,age) OUTPUT INSERTED.id VALUES (?, ?, ?), (?, ?, ?)",
          "INSERT INTO Person (id,name,age) OUTPUT INSERTED.id VALUES (?, ?, ?)"
        )

      def noIdReturning(executionType: ExecutionType) =
        makeRowNoIds(executionType)(
          "INSERT INTO Person (name,age) VALUES (?, ?), (?, ?)",
          "INSERT INTO Person (name,age) VALUES (?, ?)"
        )

      "postgres - regular/returning" in {
        val ctx: MirrorContext[PostgresDialect, Literal] = new MirrorContext(PostgresDialect, Literal)
        import ctx._
        ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2).tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
        ctx.run(liftQuery(people).foreach(p => insertPeople(p).returning(_.id)), 2).tripleBatchMulti mustEqual expectPostgresReturning(ExecutionType.Unknown)
      }
      "sqlserver - regular/returning" in {
        val ctx: MirrorContext[SQLServerDialect, Literal] = new MirrorContext(SQLServerDialect, Literal)
        import ctx._
        ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2).tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
        ctx.run(liftQuery(people).foreach(p => insertPeople(p).returning(_.id)), 2).tripleBatchMulti mustEqual expectSqlServerReturning(ExecutionType.Unknown)
      }
      "mysql - regular/returning" in {
        val ctx: MirrorContext[MySQLDialect, Literal] = new MirrorContext(MySQLDialect, Literal)
        import ctx._
        ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2).tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
        ctx.run(liftQuery(people).foreach(p => insertPeople(p).returningGenerated(_.id)), 2).tripleBatchMulti mustEqual noIdReturning(ExecutionType.Unknown)
      }
      "h2 - regular/returning" in {
        val ctx: MirrorContext[H2Dialect, Literal] = new MirrorContext(H2Dialect, Literal)
        import ctx._
        ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2).tripleBatchMulti mustEqual expectH2(ExecutionType.Unknown)
        ctx.run(liftQuery(people).foreach(p => insertPeople(p).returningGenerated(_.id)), 2).tripleBatchMulti mustEqual expectH2Returning(ExecutionType.Unknown)
      }
      "sqlite - only regular" in {
        val ctx: MirrorContext[SqliteDialect, Literal] = new MirrorContext(SqliteDialect, Literal)
        import ctx._
        ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2).tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
      }
    }

    "fallback for non-supported context" - {
      val people = List(Person(1, "A", 111), Person(2, "B", 222), Person(3, "C", 333), Person(4, "D", 444), Person(5, "E", 555))
      def expect(executionType: ExecutionType) =
        List(
          (
            "INSERT INTO Person (id,name,age) VALUES (?, ?, ?)",
            List(List(1, "A", 111), List(2, "B", 222), List(3, "C", 333), List(4, "D", 444), List(5, "E", 555)),
            executionType
          )
        )

      "oracle" - {
        val ctx: MirrorContext[OracleDialect, Literal] = new MirrorContext[OracleDialect, Literal](OracleDialect, Literal)
        import ctx._
        "static" in {
          val static = ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p))), 2)
          static.tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
        }
      }
      "sqlite - with returning clause" - {
        val ctx: MirrorContext[OracleDialect, Literal] = new MirrorContext[OracleDialect, Literal](OracleDialect, Literal)
        import ctx._
        "static" in {
          val static = ctx.run(quote(liftQuery(people).foreach(p => insertPeople(p).returning(_.id))), 2)
          static.tripleBatchMulti mustEqual expect(ExecutionType.Unknown)
        }
      }
    }
  }
}
