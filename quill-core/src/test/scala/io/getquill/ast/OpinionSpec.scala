package io.getquill.ast

import io.getquill.Spec
import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.Renameable.neutral
import io.getquill.ast.Visibility.Visible

class OpinionSpec extends Spec {

  "properties should neutralize" - {
    "to renameable default" in {
      Property.Opinionated(Ident("foo"), "bar", Fixed, Visible).neutralize mustEqual (Property.Opinionated(Ident("foo"), "bar", neutral, Visible))
    }
    "to renameable default when nested" in {
      Property.Opinionated(Property.Opinionated(Ident("foo"), "bar", Fixed, Visible), "baz", Fixed, Visible).neutralize mustEqual (
        Property.Opinionated(Property.Opinionated(Ident("foo"), "bar", neutral, Visible), "baz", neutral, Visible)
      )
    }
    "when inside other AST elements" in {
      Map(Property.Opinionated(Ident("foo"), "bar", Fixed, Visible), Ident("v"), Property.Opinionated(Ident("v"), "prop", Fixed, Visible)).neutralize mustEqual (
        Map(Property.Opinionated(Ident("foo"), "bar", neutral, Visible), Ident("v"), Property.Opinionated(Ident("v"), "prop", neutral, Visible))
      )
    }
  }

  "entities should neutralize" - {
    "to renameable default" in {
      Entity.Opinionated("foo", Nil, QEP, Fixed).neutralize mustEqual (Entity("foo", Nil, QEP))
    }
    "when inside other AST elements" in {
      Map(Entity.Opinionated("foo", Nil, QEP, Fixed), Ident("v"), Property.Opinionated(Ident("v"), "prop", Fixed, Visible)).neutralize mustEqual (
        Map(Entity("foo", Nil, QEP), Ident("v"), Property(Ident("v"), "prop"))
      )
    }
  }
}
