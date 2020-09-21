package io.getquill.context.sql

import io.getquill.Spec

class OptionSqlSpec extends Spec {
  import testContext._
  case class Someone(name: Option[String])

  "Should correctly express optional rows to SQL" - {
    "forAll" in {
      testContext.run { query[Someone].filter(s => s.name.forall(n => n == "Joe")) }.string mustEqual
        "SELECT s.name FROM Someone s WHERE s.name IS NULL OR s.name = 'Joe'"
    }
    "exists" in {
      testContext.run { query[Someone].filter(s => s.name.exists(n => n == "Joe")) }.string mustEqual
        "SELECT s.name FROM Someone s WHERE s.name = 'Joe'"
    }
    "map" in {
      testContext.run { query[Someone].map(s => s.name.map(n => n + " Bloggs")) }.string mustEqual
        "SELECT s.name || ' Bloggs' FROM Someone s"
    }
    "flatten" in {
      testContext.run { query[Someone].map(q => Some(q)).map(os => os.map(s => s.name.map(n => n + " Bloggs")).flatten) }.string mustEqual
        "SELECT q.name || ' Bloggs' FROM Someone q"
    }
    "flatMap" in {
      testContext.run { query[Someone].map(q => Some(q)).map(os => os.flatMap(s => s.name.map(n => n + " Bloggs"))) }.string mustEqual
        "SELECT q.name || ' Bloggs' FROM Someone q"
    }
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
}
