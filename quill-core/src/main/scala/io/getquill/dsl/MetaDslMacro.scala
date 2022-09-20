package io.getquill.dsl

import io.getquill.util.MacroContextExt._
import io.getquill.util.OptionalTypecheck

import scala.reflect.macros.whitebox.{Context => MacroContext}

class MetaDslMacro(val c: MacroContext) extends ValueComputation {
  import c.universe._

  def schemaMeta[T](entity: Tree, columns: Tree*)(implicit t: WeakTypeTag[T]): Tree =
    c.untypecheck {
      q"""
        new ${c.prefix}.SchemaMeta[$t] {
          private[this] val _entity = ${c.prefix}.quote {
            ${c.prefix}.querySchema[$t]($entity, ..$columns)
          }
          def entity = _entity
        }
      """
    }

  def queryMeta[T, R](expand: Tree)(extract: Tree)(implicit t: WeakTypeTag[T], r: WeakTypeTag[R]): Tree =
    c.untypecheck {
      q"""
        new ${c.prefix}.QueryMeta[$t] {
          private[this] val _expand = $expand
          def expand = _expand
          val extract =
            (r: ${c.prefix}.ResultRow, s: ${c.prefix}.Session) => $extract(implicitly[${c.prefix}.QueryMeta[$r]].extract(r, s))
        }
      """
    }

  def insertMeta[T](exclude: Tree*)(implicit t: WeakTypeTag[T]): Tree =
    actionMeta[T](value("Encoder", t.tpe, exclude: _*), "insert")

  def updateMeta[T](exclude: Tree*)(implicit t: WeakTypeTag[T]): Tree =
    actionMeta[T](value("Encoder", t.tpe, exclude: _*), "update")

  def materializeQueryMeta[T](implicit t: WeakTypeTag[T]): Tree = {
    val value = this.value("Decoder", t.tpe)
    q"""
      new ${c.prefix}.QueryMeta[$t] {
        private[this] val _expanded = ${expandQuery[T](value)}
        def expand = _expanded
        val extract = ${extract[T](value)}
      }
    """
  }

  def materializeUpdateMeta[T](implicit t: WeakTypeTag[T]): Tree =
    actionMeta[T](value("Encoder", t.tpe), "update")

  def materializeInsertMeta[T](implicit t: WeakTypeTag[T]): Tree =
    actionMeta[T](value("Encoder", t.tpe), "insert")

  def materializeSchemaMeta[T](implicit t: WeakTypeTag[T]): Tree =
    if (t.tpe.typeSymbol.isClass && t.tpe.typeSymbol.asClass.isCaseClass) {
      q"""
          new ${c.prefix}.SchemaMeta[$t] {
            private[this] val _entity =
              ${c.prefix}.quote(${c.prefix}.impliedQuerySchema[$t](${t.tpe.typeSymbol.name.decodedName.toString}))
            def entity = _entity
          }
        """
    } else {
      c.fail(
        s"Can't materialize a `SchemaMeta` for non-case-class type '${t.tpe}', please provide an implicit `SchemaMeta`."
      )
    }

  private def expandQuery[T](value: Value)(implicit t: WeakTypeTag[T]) = {
    val elements = flatten(q"x", value)
    if (elements.isEmpty)
      c.fail(s"Case class type ${t.tpe} has no values")
    // q"${c.prefix}.quote((q: io.getquill.Query[$t]) => q.map(x => io.getquill.dsl.UnlimitedTuple(..$elements)))"
    q"${c.prefix}.quote((q: io.getquill.Query[$t]) => q.map(x => x))"
  }

  private def extract[T](value: Value)(implicit t: WeakTypeTag[T]): Tree = {
    var index = -1

    val isNull =
      OptionalTypecheck(c)(q"implicitly[${c.prefix}.NullChecker]") match {
        case Some(nullChecker) => nullChecker
        case None =>
          c.fail(
            s"""Can't find a NullChecker for the context ${show(c.prefix.tree)}. // actualType.widen
               |Have you imported the context e.g.
               |import ${show(c.prefix.tree)}._""".stripMargin
          )
      }

    case class FieldExpansion(lookup: Tree, nullChecker: Tree)

    def expandRecurse(value: Value): FieldExpansion =
      value match {

        case Scalar(_, tpe, decoder, optional) =>
          index += 1
          FieldExpansion(q"$decoder($index, row, session)", q"!$isNull($index, row)")

        case Nested(_, tpe, params, optional) =>
          if (optional) {
            val dualGroups: Seq[List[(FieldExpansion, Boolean)]] =
              params.map(_.map(v => expandRecurse(v) -> v.nestedAndOptional))

            // E.g. for Person("Joe", 123) the decoder(0,row,session), decoder(1,row,session) columns
            // that turn into Decoder[String]("Joe"), Decoder[Int](123) respectively.
            // (or for Option[T] columns: Decoder[Option[String]]("Joe")))
            // Once you are in a product that has a product inside e.g. Person(name: Name("Joe", "Bloggs"), age: 123)
            // they will be the constructor and/or any other field-decoders:
            // List((new Name(Decoder("Joe") || Decoder("Bloggs")), Decoder(123))
            // This is what needs to be fed into the constructor of the outer-entity i.e.
            // new Person((new Name(Decoder("Joe") || Decoder("Bloggs")), Decoder(123))
            val productElements: Seq[List[Tree]] = dualGroups.map(_.map { case (k, v) => (k.lookup) })
            // E.g. for Person("Joe", 123) the List(q"!nullChecker(0,row)", q"!nullChecker(1,row)") columns
            // that eventually turn into List(!NullChecker("Joe"), !NullChecker(123)) columns.
            // Once you are in a product that has a product inside e.g. Person(name: Name("Joe", "Bloggs"), age: 123)
            // they will be the concatonations of the Or-clauses e.g.
            // List( (NullChecker("Joe") || NullChecker("Bloggs")), NullChecker(123))
            // This is what needs to be the null-checker of the outer entity i.e.
            // if ((NullChecker("Joe") || NullChecker("Bloggs")) || NullChecker(123)) Some(new Name(...)) else None
            val nullChecks: Seq[List[Tree]] = dualGroups.map(_.map { case (k, v) => (k.nullChecker) })

            // Or-s together the NullChecker columns.
            // E.g. for Person("Joe", 123)
            // !NullChecker("Joe") || !NullChecker(123)
            val allColumnsNotNull = nullChecks.flatMap(v => v).reduce((a, b) => q"$a || $b")
            val newProductCreate  = q"if ($allColumnsNotNull) Some(new $tpe(...${productElements})) else None"

            FieldExpansion(newProductCreate, allColumnsNotNull)
          } else {
            val dualGroups                        = params.map(_.map(expandRecurse(_)))
            val nullChecks: Seq[List[Tree]]       = dualGroups.map(_.map(_.nullChecker))
            val productElements: List[List[Tree]] = dualGroups.map(_.map(_.lookup))

            val lookup            = q"new $tpe(...${productElements})"
            val allColumnsNotNull = nullChecks.flatMap(v => v).reduce((a, b) => q"$a || $b")

            FieldExpansion(
              lookup,
              // Propagate up the null checks found here. Even though this row cannot be null, maybe a higher-level
              // row is null. E.g. we are processing:
              //   Some((Person(name:String, age:Int), Address(street:Option[String]))
              // from the row:
              //   Row(null, null, null) which becomes Option((Person(null,0), Address(None)))
              // and say we are processing the 'Address' part which can't be null. We still want to
              // return the internal columns of Address since the outer Option can be None.
              // Address.
              allColumnsNotNull
            )
          }
      }
    q"(row: ${c.prefix}.ResultRow, session: ${c.prefix}.Session) => ${expandRecurse(value).lookup}"
  }

  private def actionMeta[T](value: Value, method: String)(implicit t: WeakTypeTag[T]) = {
    val assignments =
      flatten(q"v", value)
        .zip(flatten(q"value", value))
        .map { case (vTree, valueTree) =>
          q"(v: $t) => $vTree -> $valueTree"
        }
    c.untypecheck {
      q"""
        new ${c.prefix}.${TypeName(method.capitalize + "Meta")}[$t] {
          private[this] val _expand =
            ${c.prefix}.quote((q: io.getquill.EntityQuery[$t], value: $t) => q.${TermName(method)}(..$assignments))
          def expand = _expand
        }
      """
    }
  }
}
