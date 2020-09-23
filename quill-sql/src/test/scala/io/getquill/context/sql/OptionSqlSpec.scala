package io.getquill.context.sql

import io.getquill.{ MirrorSqlDialectWithBooleanLiterals, Spec }

class OptionSqlSpec extends Spec {
  import testContext._
  case class Someone(name: Option[String])

  "Should correctly express optional rows to SQL" - {
    "forAll" in {
      testContext.run {
        query[Someone].filter(s => s.name.forall(n => n == "Joe"))
      }.string mustEqual
        "SELECT s.name FROM Someone s WHERE s.name IS NULL OR s.name = 'Joe'"
    }
    "exists" in {
      testContext.run {
        query[Someone].filter(s => s.name.exists(n => n == "Joe"))
      }.string mustEqual
        "SELECT s.name FROM Someone s WHERE s.name = 'Joe'"
    }
    "map" in {
      testContext.run {
        query[Someone].map(s => s.name.map(n => n + " Bloggs"))
      }.string mustEqual
        "SELECT s.name || ' Bloggs' FROM Someone s"
    }
    "flatten" in {
      testContext.run {
        query[Someone].map(q => Some(q)).map(os => os.map(s => s.name.map(n => n + " Bloggs")).flatten)
      }.string mustEqual
        "SELECT q.name || ' Bloggs' FROM Someone q"
    }
    "flatMap" in {
      testContext.run {
        query[Someone].map(q => Some(q)).map(os => os.flatMap(s => s.name.map(n => n + " Bloggs")))
      }.string mustEqual
        "SELECT q.name || ' Bloggs' FROM Someone q"
    }
  }
  "Should correctly express optional tables to SQL" - {
    "table flatMap" in {
      testContext.run { query[Someone].map(q => Some(q)).flatMap(os => query[Someone].join(s1 => s1.name == os.flatMap(oss => oss.name))) }.string mustEqual
        "SELECT s1.name FROM Someone q INNER JOIN Someone s1 ON s1.name IS NULL AND q.name IS NULL OR s1.name = q.name"
    }

    case class Node(name: String, uptime: Long)

    "table foreach" in {
      testContext.run { query[Node].map(n => Some(n)).map(n => n.exists(nn => nn.uptime > 123)) }.string mustEqual
        "SELECT n.uptime > 123 FROM Node n"
    }

    "table forall" in {
      testContext.run { query[Node].map(n => Some(n)).map(n => n.exists(nn => nn.uptime > 123)) }.string mustEqual
        "SELECT n.uptime > 123 FROM Node n"
    }
  }

  case class Node(name: String, isUp: Option[Boolean])

  "getOrElse with Booleans" - {
    "constant" - {
      "in map clause" - {
        "normally" in {
          testContext.run { query[Node].map(s => s.isUp.map(n => n == true).getOrElse(false)) }.string mustEqual
            "SELECT (s.isUp = true) IS NOT NULL AND s.isUp = true OR false FROM Node s"
        }
        "map to self" in {
          testContext.run { query[Node].map(s => s.isUp.map(n => n).getOrElse(false)) }.string mustEqual
            "SELECT s.isUp IS NOT NULL AND s.isUp OR false FROM Node s"
        }
        "simple" in {
          testContext.run { query[Node].map(s => s.isUp.getOrElse(false)) }.string mustEqual
            "SELECT s.isUp IS NOT NULL AND s.isUp OR false FROM Node s"
        }
      }
      "in filter clause" - {
        "normally" in {
          testContext.run { query[Node].filter(s => s.isUp.map(n => n == true).getOrElse(false)) }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE (s.isUp = true) IS NOT NULL AND s.isUp = true OR false"
        }
        "map to self" in {
          testContext.run { query[Node].filter(s => s.isUp.map(n => n).getOrElse(false)) }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE s.isUp IS NOT NULL AND s.isUp OR false"
        }
        "simple" in {
          testContext.run { query[Node].filter(s => s.isUp.getOrElse(false)) }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE s.isUp IS NOT NULL AND s.isUp OR false"
        }
      }
    }
    "liftable" - {
      "in map clause" - {
        "normally" in {
          testContext.run { query[Node].map(s => s.isUp.map(n => n == true).getOrElse(lift(false))) }.string mustEqual //hello
            "SELECT (s.isUp = true) IS NOT NULL AND s.isUp = true OR ? FROM Node s"
        }
        "map to self" in {
          testContext.run { query[Node].map(s => s.isUp.map(n => n).getOrElse(lift(false))) }.string mustEqual
            "SELECT s.isUp IS NOT NULL AND s.isUp OR ? FROM Node s"
        }
        "simple" in {
          testContext.run { query[Node].map(s => s.isUp.getOrElse(lift(false))) }.string mustEqual
            "SELECT s.isUp IS NOT NULL AND s.isUp OR ? FROM Node s"
        }
      }
      "in filter clause" - {
        "normally" in {
          testContext.run { query[Node].filter(s => s.isUp.map(n => n == true).getOrElse(lift(false))) }.string mustEqual //hello
            "SELECT s.name, s.isUp FROM Node s WHERE (s.isUp = true) IS NOT NULL AND s.isUp = true OR ?"
        }
        "map to self" in {
          testContext.run { query[Node].filter(s => s.isUp.map(n => n).getOrElse(lift(false))) }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE s.isUp IS NOT NULL AND s.isUp OR ?"
        }
        "simple" in {
          testContext.run { query[Node].filter(s => s.isUp.getOrElse(lift(false))) }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE s.isUp IS NOT NULL AND s.isUp OR ?"
        }
      }
    }
  }

  "getOrElse with Booleans Literal Expansion" - testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { testContext =>
    import testContext._

    "constant" - {
      "in map clause" - {
        "normally" in {
          testContext.run {
            query[Node].map(s => s.isUp.map(n => n == true).getOrElse(false))
          }.string mustEqual
            "SELECT CASE WHEN CASE WHEN s.isUp = 1 THEN 1 ELSE 0 END IS NOT NULL AND s.isUp = 1 OR 1 = 0 THEN 1 ELSE 0 END FROM Node s"
        }
        "map to self" in {
          testContext.run {
            query[Node].map(s => s.isUp.map(n => n).getOrElse(false))
          }.string mustEqual
            "SELECT CASE WHEN s.isUp IS NOT NULL AND 1 = s.isUp OR 1 = 0 THEN 1 ELSE 0 END FROM Node s"
        }
        "simple" in {
          testContext.run {
            query[Node].map(s => s.isUp.getOrElse(false))
          }.string mustEqual
            "SELECT CASE WHEN s.isUp IS NOT NULL AND 1 = s.isUp OR 1 = 0 THEN 1 ELSE 0 END FROM Node s"
        }
      }
      "in filter clause" - {
        "normally" in {
          testContext.run {
            query[Node].filter(s => s.isUp.map(n => n == true).getOrElse(false))
          }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE CASE WHEN s.isUp = 1 THEN 1 ELSE 0 END IS NOT NULL AND s.isUp = 1 OR 1 = 0"
        }
        "map to self" in {
          testContext.run {
            query[Node].filter(s => s.isUp.map(n => n).getOrElse(false))
          }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE s.isUp IS NOT NULL AND 1 = s.isUp OR 1 = 0"
        }
        "simple" in {
          testContext.run {
            query[Node].filter(s => s.isUp.getOrElse(false))
          }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE s.isUp IS NOT NULL AND 1 = s.isUp OR 1 = 0"
        }
      }
    }
    "liftable" - {
      "in map clause" - {
        "normally" in {
          testContext.run {
            query[Node].map(s => s.isUp.map(n => n == true).getOrElse(lift(false)))
          }.string mustEqual
            "SELECT CASE WHEN CASE WHEN s.isUp = 1 THEN 1 ELSE 0 END IS NOT NULL AND s.isUp = 1 OR 1 = ? THEN 1 ELSE 0 END FROM Node s"
        }
        "map to self" in {
          testContext.run {
            query[Node].map(s => s.isUp.map(n => n).getOrElse(lift(false)))
          }.string mustEqual
            "SELECT CASE WHEN s.isUp IS NOT NULL AND 1 = s.isUp OR 1 = ? THEN 1 ELSE 0 END FROM Node s"
        }
        "simple" in {
          testContext.run {
            query[Node].map(s => s.isUp.getOrElse(lift(false)))
          }.string mustEqual
            "SELECT CASE WHEN s.isUp IS NOT NULL AND 1 = s.isUp OR 1 = ? THEN 1 ELSE 0 END FROM Node s"
        }
      }
      "in filter clause" - {
        "normally" in {
          testContext.run {
            query[Node].filter(s => s.isUp.map(n => n == true).getOrElse(lift(false)))
          }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE CASE WHEN s.isUp = 1 THEN 1 ELSE 0 END IS NOT NULL AND s.isUp = 1 OR 1 = ?"
        }
        "map to self" in {
          testContext.run {
            query[Node].filter(s => s.isUp.map(n => n).getOrElse(lift(false)))
          }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE s.isUp IS NOT NULL AND 1 = s.isUp OR 1 = ?"
        }
        "simple" in {
          testContext.run {
            query[Node].filter(s => s.isUp.getOrElse(lift(false)))
          }.string mustEqual
            "SELECT s.name, s.isUp FROM Node s WHERE s.isUp IS NOT NULL AND 1 = s.isUp OR 1 = ?"
        }
      }
    }
  }

}
