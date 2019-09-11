package io.getquill.ast

import io.getquill.Spec
import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.Renameable.neutral

class OpinionSpec extends Spec {

  "properties should neutralize" - {
    "to renameable default" in {
      Property.Opinionated(Ident("foo"), "bar", Fixed).neutralize mustEqual (Property.Opinionated(Ident("foo"), "bar", neutral))
    }
    "to renameable default when nested" in {
      Property.Opinionated(Property.Opinionated(Ident("foo"), "bar", Fixed), "baz", Fixed).neutralize mustEqual (
        Property.Opinionated(Property.Opinionated(Ident("foo"), "bar", neutral), "baz", neutral)
      )
    }
    "when inside other AST elements" in {
      Map(Property.Opinionated(Ident("foo"), "bar", Fixed), Ident("v"), Property.Opinionated(Ident("v"), "prop", Fixed)).neutralize mustEqual (
        Map(Property.Opinionated(Ident("foo"), "bar", neutral), Ident("v"), Property.Opinionated(Ident("v"), "prop", neutral))
      )
    }
  }

  "entities should neutralize" - {
    "to renameable default" in {
      Entity.Opinionated("foo", Nil, Fixed).neutralize mustEqual (Entity("foo", Nil))
    }
    "when inside other AST elements" in {
      Map(Entity.Opinionated("foo", Nil, Fixed), Ident("v"), Property.Opinionated(Ident("v"), "prop", Fixed)).neutralize mustEqual (
        Map(Entity("foo", Nil), Ident("v"), Property(Ident("v"), "prop"))
      )
    }
  }
}
