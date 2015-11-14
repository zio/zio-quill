//package io.getquill.source.sql
//
//import io.getquill._
//import io.getquill.norm.Normalize
//import io.getquill.source.sql.naming.Literal
//
//class ExpandNestedQueriesSpec extends Spec {
//
//  "test" in {
//    val j = quote {
//      for {
//        a <- qr1
//        b <- qr2
//      } yield {
//        (a, b)
//      }
//    }
//    val q = quote {
//      j.union(j).map(u => u._1.s)
//    }
//    import idiom.FallbackDialect._
//    import io.getquill.util.Show._
//    implicit val strategy = Literal
//    println(ExpandNestedQueries(SqlQuery(Normalize(q.ast)), Nil).show)
//  }
//}