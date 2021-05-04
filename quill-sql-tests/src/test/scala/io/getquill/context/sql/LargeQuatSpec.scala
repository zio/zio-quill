package io.getquill.context.sql

import io.getquill.Spec

class LargeQuatSpec extends Spec {

  case class Large(
    rec1:  String,
    rec2:  String,
    rec3:  String,
    rec4:  String,
    rec5:  String,
    rec6:  String,
    rec7:  String,
    rec8:  String,
    rec9:  String,
    rec10: String,
    rec11: String,
    rec12: String,
    rec13: String,
    rec14: String,
    rec15: String,
    rec16: String,
    rec17: String,
    rec18: String,
    rec19: String,
    rec20: String,
    rec21: String,
    rec22: String,
    rec23: String,
    rec24: String,
    rec25: String,
    rec26: String,
    rec27: String,
    rec28: String,
    rec29: String,
    rec30: String,
    rec31: String,
    rec32: String,
    rec33: String,
    rec34: String,
    rec35: String,
    rec36: String,
    rec37: String,
    rec38: String,
    rec39: String,
    rec40: String,
    rec41: String,
    rec42: String,
    rec43: String,
    rec44: String,
    rec45: String,
    rec46: String,
    rec47: String,
    rec48: String,
    rec49: String,
    rec50: String
  )

  "large record size quats compile" in {
    import testContext._

    val q = quote {
      for {
        a <- query[Large]
        b <- query[Large].join(b => b.rec1 == a.rec1)
        c <- query[Large].join(c => c.rec1 == b.rec1)
        d <- query[Large].join(d => d.rec1 == c.rec1)
      } yield (a, b, c, d)
    }

    testContext.run(q).string mustEqual
      "SELECT a.rec1, a.rec2, a.rec3, a.rec4, a.rec5, a.rec6, a.rec7, a.rec8, a.rec9, a.rec10, a.rec11, a.rec12, a.rec13, a.rec14, a.rec15, a.rec16, a.rec17, a.rec18, a.rec19, a.rec20, a.rec21, a.rec22, a.rec23, a.rec24, a.rec25, a.rec26, a.rec27, a.rec28, a.rec29, a.rec30, a.rec31, a.rec32, a.rec33, a.rec34, a.rec35, a.rec36, a.rec37, a.rec38, a.rec39, a.rec40, a.rec41, a.rec42, a.rec43, a.rec44, a.rec45, a.rec46, a.rec47, a.rec48, a.rec49, a.rec50, b.rec1, b.rec2, b.rec3, b.rec4, b.rec5, b.rec6, b.rec7, b.rec8, b.rec9, b.rec10, b.rec11, b.rec12, b.rec13, b.rec14, b.rec15, b.rec16, b.rec17, b.rec18, b.rec19, b.rec20, b.rec21, b.rec22, b.rec23, b.rec24, b.rec25, b.rec26, b.rec27, b.rec28, b.rec29, b.rec30, b.rec31, b.rec32, b.rec33, b.rec34, b.rec35, b.rec36, b.rec37, b.rec38, b.rec39, b.rec40, b.rec41, b.rec42, b.rec43, b.rec44, b.rec45, b.rec46, b.rec47, b.rec48, b.rec49, b.rec50, c.rec1, c.rec2, c.rec3, c.rec4, c.rec5, c.rec6, c.rec7, c.rec8, c.rec9, c.rec10, c.rec11, c.rec12, c.rec13, c.rec14, c.rec15, c.rec16, c.rec17, c.rec18, c.rec19, c.rec20, c.rec21, c.rec22, c.rec23, c.rec24, c.rec25, c.rec26, c.rec27, c.rec28, c.rec29, c.rec30, c.rec31, c.rec32, c.rec33, c.rec34, c.rec35, c.rec36, c.rec37, c.rec38, c.rec39, c.rec40, c.rec41, c.rec42, c.rec43, c.rec44, c.rec45, c.rec46, c.rec47, c.rec48, c.rec49, c.rec50, d.rec1, d.rec2, d.rec3, d.rec4, d.rec5, d.rec6, d.rec7, d.rec8, d.rec9, d.rec10, d.rec11, d.rec12, d.rec13, d.rec14, d.rec15, d.rec16, d.rec17, d.rec18, d.rec19, d.rec20, d.rec21, d.rec22, d.rec23, d.rec24, d.rec25, d.rec26, d.rec27, d.rec28, d.rec29, d.rec30, d.rec31, d.rec32, d.rec33, d.rec34, d.rec35, d.rec36, d.rec37, d.rec38, d.rec39, d.rec40, d.rec41, d.rec42, d.rec43, d.rec44, d.rec45, d.rec46, d.rec47, d.rec48, d.rec49, d.rec50 FROM Large a INNER JOIN Large b ON b.rec1 = a.rec1 INNER JOIN Large c ON c.rec1 = b.rec1 INNER JOIN Large d ON d.rec1 = c.rec1"
  }

}
