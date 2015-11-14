package io.getquill.source.sql.idiom

import io.getquill._
import io.getquill.source.sql.mirror.mirrorSource

class Delivery {
  case class Delivery(id: Long, dname: String, messageId: Long, sentAt: Long)

  case class Message(id: Long, mname: String, mbody: String)

  val allNotFinished = quote {
    (for {
      d <- query[Delivery]
      m <- query[Message] if d.messageId == m.id
    } yield (d, m))
      .filter { case (d, m) => d.sentAt >= 1400000000L }
  }

  val allFinishedWithMessageName = quote {
    (messageName: String) =>
      (for {
        d <- query[Delivery]
        m <- query[Message] if d.messageId == m.id && m.mname == messageName
      } yield (d, m))
        .filter { case (d, m) => d.sentAt < 1400000000L }
  }

  val q = quote {
    allNotFinished.union(allFinishedWithMessageName("Boo")).sortBy(_._1.sentAt).map(_._1.id)
  }

  mirrorSource.run(q)

  case class TestInstance(id: String)

  val q2 = quote {
    query[TestInstance].groupBy(_.id).map {
      case (c, tbl) =>
        (c, tbl.size)
    }
  }
  
  mirrorSource.run(q2.size)
}