package io.getquill.ast

import io.getquill.util.Show._

object ParametrizedShow {

  import ExprShow._
  import QueryShow._

  implicit val parametrizedShow: Show[Parametrized] = new Show[Parametrized] {
    def show(p: Parametrized) =
      s"(${p.params.mkString(", ")}) => ${bodyString(p)}"
  }

  private def bodyString(p: Parametrized) =
    p match {
      case ParametrizedQuery(_, q) => q.show
      case ParametrizedExpr(_, e)  => e.show
    }
}
