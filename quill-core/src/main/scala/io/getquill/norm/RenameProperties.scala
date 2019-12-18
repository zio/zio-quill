package io.getquill.norm

import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.Visibility.Visible
import io.getquill.ast._
import io.getquill.util.Interpolator
import io.getquill.util.Messages.TraceType.Normalizations

object RenameProperties extends StatelessTransformer {
  val interp = new Interpolator(Normalizations, 3)
  import interp._
  def traceDifferent(one: Any, two: Any) =
    if (one != two)
      trace"Replaced $one with $two".andLog()
    else
      trace"Replacements did not match".andLog()

  override def apply(q: Query): Query =
    applySchemaOnly(q)

  override def apply(q: Action): Action =
    applySchema(q) match {
      case (q, schema) => q
    }

  override def apply(e: Operation): Operation =
    e match {
      case UnaryOperation(o, c: Query) =>
        UnaryOperation(o, applySchemaOnly(apply(c)))
      case _ => super.apply(e)
    }

  private def applySchema(q: Action): (Action, Schema) =
    q match {
      case Insert(q: Query, assignments) => applySchema(q, assignments, Insert)
      case Update(q: Query, assignments) => applySchema(q, assignments, Update)
      case Delete(q: Query) =>
        applySchema(q) match {
          case (q, schema) => (Delete(q), schema)
        }
      case Returning(action: Action, alias, body) =>
        applySchema(action) match {
          case (action, schema) =>
            val replace =
              trace"Finding Replacements for $body inside $alias using schema $schema:" andReturn
                replacements(alias, schema)
            val bodyr = BetaReduction(body, replace: _*)
            traceDifferent(body, bodyr)
            (Returning(action, alias, bodyr), schema)
        }
      case ReturningGenerated(action: Action, alias, body) =>
        applySchema(action) match {
          case (action, schema) =>
            val replace =
              trace"Finding Replacements for $body inside $alias using schema $schema:" andReturn
                replacements(alias, schema)
            val bodyr = BetaReduction(body, replace: _*)
            traceDifferent(body, bodyr)
            (ReturningGenerated(action, alias, bodyr), schema)
        }
      case OnConflict(a: Action, target, act) =>
        applySchema(a) match {
          case (action, schema) =>
            val targetR = target match {
              case OnConflict.Properties(props) =>
                val propsR = props.map { prop =>
                  val replace =
                    trace"Finding Replacements for $props inside ${prop.ast} using schema $schema:" andReturn
                      replacements(prop.ast, schema) // A BetaReduction on a Property will always give back a Property
                  BetaReduction(prop, replace: _*).asInstanceOf[Property]
                }
                traceDifferent(props, propsR)
                OnConflict.Properties(propsR)
              case OnConflict.NoTarget => target
            }
            val actR = act match {
              case OnConflict.Update(assignments) =>
                OnConflict.Update(replaceAssignments(assignments, schema))
              case _ => act
            }
            (OnConflict(action, targetR, actR), schema)
        }
      case q => (q, TupleSchema.empty)
    }

  private def replaceAssignments(a: List[Assignment], schema: Schema): List[Assignment] =
    a.map {
      case Assignment(alias, prop, value) =>
        val replace =
          trace"Finding Replacements for $prop inside $alias using schema $schema:" andReturn
            replacements(alias, schema)
        val propR = BetaReduction(prop, replace: _*)
        traceDifferent(prop, propR)
        val valueR = BetaReduction(value, replace: _*)
        traceDifferent(value, valueR)
        Assignment(alias, propR, valueR)
    }

  private def applySchema(q: Query, a: List[Assignment], f: (Query, List[Assignment]) => Action): (Action, Schema) =
    applySchema(q) match {
      case (q, schema) =>
        (f(q, replaceAssignments(a, schema)), schema)
    }

  private def applySchemaOnly(q: Query): Query =
    applySchema(q) match {
      case (q, _) => q
    }

  object TupleIndex {
    def unapply(s: String): Option[Int] =
      if (s.matches("_[0-9]*"))
        Some(s.drop(1).toInt - 1)
      else
        None
  }

  sealed trait Schema {
    def lookup(property: List[String]): Option[Schema] =
      (property, this) match {
        case (Nil, schema) =>
          trace"Nil at $property returning " andReturn
            Some(schema)
        case (path, e @ EntitySchema(_)) =>
          trace"Entity at $path returning " andReturn
            Some(e.subSchemaOrEmpty(path))
        case (head :: tail, CaseClassSchema(props)) if (props.contains(head)) =>
          trace"Case class at $property returning " andReturn
            props(head).lookup(tail)
        case (TupleIndex(idx) :: tail, TupleSchema(values)) if values.contains(idx) =>
          trace"Tuple at at $property returning " andReturn
            values(idx).lookup(tail)
        case _ =>
          trace"Nothing found at $property returning " andReturn
            None
      }
  }

  // Represents a nested property path to an identity i.e. Property(Property(... Ident(), ...))
  object PropertyMatroshka {

    def traverse(initial: Property): Option[(Ident, List[String])] =
      initial match {
        // If it's a nested-property walk inside and append the name to the result (if something is returned)
        case Property(inner: Property, name) =>
          traverse(inner).map { case (id, list) => (id, list :+ name) }
        // If it's a property with ident in the core, return that
        case Property(id: Ident, name) =>
          Some((id, List(name)))
        // Otherwise an ident property is not inside so don't return anything
        case _ =>
          None
      }

    def unapply(ast: Ast): Option[(Ident, List[String])] =
      ast match {
        case p: Property => traverse(p)
        case _           => None
      }

  }

  def protractSchema(body: Ast, ident: Ident, schema: Schema): Option[Schema] = {

    def protractSchemaRecurse(body: Ast, schema: Schema): Option[Schema] =
      body match {
        // if any values yield a sub-schema which is not an entity, recurse into that
        case cc @ CaseClass(values) =>
          trace"Protracting CaseClass $cc into new schema:" andReturn
            CaseClassSchema(
              values.collect {
                case (name, innerBody @ HierarchicalAstEntity())          => (name, protractSchemaRecurse(innerBody, schema))
                // pass the schema into a recursive call an extract from it when we non tuple/caseclass element
                case (name, innerBody @ PropertyMatroshka(`ident`, path)) => (name, protractSchemaRecurse(innerBody, schema))
                // we have reached an ident i.e. recurse to pass the current schema into the case class
                case (name, `ident`)                                      => (name, protractSchemaRecurse(ident, schema))
              }.collect {
                case (name, Some(subSchema)) => (name, subSchema)
              }
            ).notEmpty
        case tup @ Tuple(values) =>
          trace"Protracting Tuple $tup into new schema:" andReturn
            TupleSchema.fromIndexes(
              values.zipWithIndex.collect {
                case (innerBody @ HierarchicalAstEntity(), index)          => (index, protractSchemaRecurse(innerBody, schema))
                // pass the schema into a recursive call an extract from it when we non tuple/caseclass element
                case (innerBody @ PropertyMatroshka(`ident`, path), index) => (index, protractSchemaRecurse(innerBody, schema))
                // we have reached an ident i.e. recurse to pass the current schema into the tuple
                case (`ident`, index)                                      => (index, protractSchemaRecurse(ident, schema))
              }.collect {
                case (index, Some(subSchema)) => (index, subSchema)
              }
            ).notEmpty

        case prop @ PropertyMatroshka(`ident`, path) =>
          trace"Protraction completed schema path $prop at the schema $schema pointing to:" andReturn
            schema match {
              //case e: EntitySchema => Some(e)
              case _ => schema.lookup(path)
            }
        case `ident` =>
          trace"Protraction completed with the mapping identity $ident at the schema:" andReturn
            Some(schema)
        case other =>
          trace"Protraction DID NOT find a sub schema, it completed with $other at the schema:" andReturn
            Some(schema)
      }

    protractSchemaRecurse(body, schema)
  }

  case object EmptySchema extends Schema
  case class EntitySchema(e: Entity) extends Schema {
    def noAliases = e.properties.isEmpty

    private def subSchema(path: List[String]) =
      EntitySchema(Entity(s"sub-${e.name}", e.properties.flatMap {
        case PropertyAlias(aliasPath, alias) =>
          if (aliasPath == path)
            List(PropertyAlias(aliasPath, alias))
          else if (aliasPath.startsWith(path))
            List(PropertyAlias(aliasPath.diff(path), alias))
          else
            List()
      }))

    def subSchemaOrEmpty(path: List[String]): Schema =
      trace"Creating sub-schema for entity $e at path $path will be" andReturn {
        val sub = subSchema(path)
        if (sub.noAliases) EmptySchema else sub
      }

  }
  case class TupleSchema(m: collection.Map[Int, Schema] /* Zero Indexed */ ) extends Schema {
    def list = m.toList.sortBy(_._1)
    def notEmpty =
      if (this.m.nonEmpty) Some(this) else None
  }
  case class CaseClassSchema(m: collection.Map[String, Schema]) extends Schema {
    def list = m.toList
    def notEmpty =
      if (this.m.nonEmpty) Some(this) else None
  }
  object CaseClassSchema {
    def apply(property: String, value: Schema): CaseClassSchema =
      CaseClassSchema(collection.Map(property -> value))
    def apply(list: List[(String, Schema)]): CaseClassSchema =
      CaseClassSchema(list.toMap)
  }

  object TupleSchema {
    def fromIndexes(schemas: List[(Int, Schema)]): TupleSchema =
      TupleSchema(schemas.toMap)

    def apply(schemas: List[Schema]): TupleSchema =
      TupleSchema(schemas.zipWithIndex.map(_.swap).toMap)

    def apply(index: Int, schema: Schema): TupleSchema =
      TupleSchema(collection.Map(index -> schema))

    def empty: TupleSchema = TupleSchema(List.empty)
  }

  case class CompoundSchema(a: Schema, b: Schema) extends Schema

  object HierarchicalAstEntity {
    def unapply(ast: Ast): Boolean =
      ast match {
        case cc: CaseClass => true
        case tup: Tuple    => true
        case _             => false
      }
  }

  private def applySchema(q: Query): (Query, Schema) = {
    q match {

      // Don't understand why this is needed....
      case Map(q: Query, x, p) =>
        applySchema(q) match {
          case (q, subSchema) =>
            val replace =
              trace"Looking for possible replacements for $p inside $x using schema $subSchema:" andReturn
                replacements(x, subSchema)
            val pr = BetaReduction(p, replace: _*)
            traceDifferent(p, pr)
            val prr = apply(pr)
            traceDifferent(pr, prr)

            val schema =
              trace"Protracting Hierarchical Entity $prr into sub-schema: $subSchema" andReturn {
                protractSchema(prr, x, subSchema)
              }.getOrElse(EmptySchema)

            (Map(q, x, prr), schema)
        }

      case e: Entity                    => (e, EntitySchema(e))
      case Filter(q: Query, x, p)       => applySchema(q, x, p, Filter)
      case SortBy(q: Query, x, p, o)    => applySchema(q, x, p, SortBy(_, _, _, o))
      case GroupBy(q: Query, x, p)      => applySchema(q, x, p, GroupBy)
      case Aggregation(op, q: Query)    => applySchema(q, Aggregation(op, _))
      case Take(q: Query, n)            => applySchema(q, Take(_, n))
      case Drop(q: Query, n)            => applySchema(q, Drop(_, n))
      case Nested(q: Query)             => applySchema(q, Nested)
      case Distinct(q: Query)           => applySchema(q, Distinct)
      case Union(a: Query, b: Query)    => applySchema(a, b, Union)
      case UnionAll(a: Query, b: Query) => applySchema(a, b, UnionAll)

      case FlatMap(q: Query, x, p) =>
        applySchema(q, x, p, FlatMap) match {
          case (FlatMap(q, x, p: Query), oldSchema) =>
            val (pr, newSchema) = applySchema(p)
            (FlatMap(q, x, pr), newSchema)
          case (flatMap, oldSchema) =>
            (flatMap, TupleSchema.empty)
        }

      case ConcatMap(q: Query, x, p) =>
        applySchema(q, x, p, ConcatMap) match {
          case (ConcatMap(q, x, p: Query), oldSchema) =>
            val (pr, newSchema) = applySchema(p)
            (ConcatMap(q, x, pr), newSchema)
          case (concatMap, oldSchema) =>
            (concatMap, TupleSchema.empty)
        }

      case Join(typ, a: Query, b: Query, iA, iB, on) =>
        (applySchema(a), applySchema(b)) match {
          case ((a, schemaA), (b, schemaB)) =>
            val combinedReplacements =
              trace"Finding Replacements for $on inside ${(iA, iB)} using schemas ${(schemaA, schemaB)}:" andReturn {
                val replaceA = replacements(iA, schemaA)
                val replaceB = replacements(iB, schemaB)
                replaceA ++ replaceB
              }
            val onr = BetaReduction(on, combinedReplacements: _*)
            traceDifferent(on, onr)
            (Join(typ, a, b, iA, iB, onr), TupleSchema(List(schemaA, schemaB)))
        }

      case FlatJoin(typ, a: Query, iA, on) =>
        applySchema(a) match {
          case (a, schemaA) =>
            val replaceA =
              trace"Finding Replacements for $on inside $iA using schema $schemaA:" andReturn
                replacements(iA, schemaA)
            val onr = BetaReduction(on, replaceA: _*)
            traceDifferent(on, onr)
            (FlatJoin(typ, a, iA, onr), schemaA)
        }

      case Map(q: Operation, x, p) if x == p =>
        (Map(apply(q), x, p), TupleSchema.empty)

      case Map(Infix(parts, params, pure), x, p) =>

        val transformed =
          params.map {
            case q: Query =>
              val (qr, schema) = applySchema(q)
              traceDifferent(q, qr)
              (qr, Some(schema))
            case q =>
              (q, None)
          }

        val schema =
          transformed.collect {
            case (_, Some(schema)) => schema
          } match {
            case e :: Nil => e
            case ls       => TupleSchema(ls)
          }
        val replace =
          trace"Finding Replacements for $p inside $x using schema $schema:" andReturn
            replacements(x, schema)
        val pr = BetaReduction(p, replace: _*)
        traceDifferent(p, pr)
        val prr = apply(pr)
        traceDifferent(pr, prr)

        (Map(Infix(parts, transformed.map(_._1), pure), x, prr), schema)

      case q =>
        (q, TupleSchema.empty)
    }
  }

  private def applySchema(ast: Query, f: Ast => Query): (Query, Schema) =
    applySchema(ast) match {
      case (ast, schema) =>
        (f(ast), schema)
    }

  private def applySchema(a: Query, b: Query, f: (Ast, Ast) => Query): (Query, Schema) = {
    val (qa, newSchema1) = applySchema(a)
    val (qb, newSchema2) = applySchema(b)
    (f(qa, qb), CompoundSchema(newSchema1, newSchema2))
  }

  private def applySchema[T](q: Query, x: Ident, p: Ast, f: (Ast, Ident, Ast) => T): (T, Schema) =
    applySchema(q) match {
      case (q, schema) =>
        val replace =
          trace"Finding Replacements for $p inside $x using schema $schema:" andReturn
            replacements(x, schema)
        val pr = BetaReduction(p, replace: _*)
        traceDifferent(p, pr)
        val prr = apply(pr)
        traceDifferent(pr, prr)
        (f(q, x, prr), schema)
    }

  private def replacements(base: Ast, schema: Schema): List[(Ast, Ast)] =
    schema match {
      // The entity renameable property should already have been marked as Fixed
      case EntitySchema(Entity(entity, properties)) =>
        //trace"%4 Entity Schema: " andReturn
        properties.flatMap {
          // A property alias means that there was either a querySchema(tableName, _.propertyName -> PropertyAlias)
          // or a schemaMeta (which ultimately gets turned into a querySchema) which is the same thing but implicit.
          // In this case, we want to rename the properties based on the property aliases as well as mark
          // them Fixed since they should not be renamed based on
          // the naming strategy wherever they are tokenized (e.g. in SqlIdiom)
          case PropertyAlias(path, alias) =>
            def apply(base: Ast, path: List[String]): Ast =
              path match {
                case Nil          => base
                case head :: tail => apply(Property(base, head), tail)
              }
            List(
              apply(base, path) -> Property.Opinionated(base, alias, Fixed, Visible) // Hidden properties cannot be renamed
            )
        }
      case tup: TupleSchema =>
        //trace"%4 Tuple Schema: " andReturn
        tup.list.flatMap {
          case (idx, value) =>
            replacements(
              // Should not matter whether property is fixed or variable here
              // since beta reduction ignores that
              Property(base, s"_${idx + 1}"),
              value
            )
        }
      case cc: CaseClassSchema =>
        //trace"%4 CaseClass Schema: " andReturn
        cc.list.flatMap {
          case (property, value) =>
            replacements(
              // Should not matter whether property is fixed or variable here
              // since beta reduction ignores that
              Property(base, property),
              value
            )
        }
      case CompoundSchema(a, b) =>
        replacements(base, a) ++ replacements(base, b)
      // Do nothing if it is an empty schema
      case EmptySchema => List()
    }
}
