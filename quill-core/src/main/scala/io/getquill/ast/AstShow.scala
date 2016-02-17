package io.getquill.ast

import io.getquill.ast
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

object AstShow {

  implicit val astShow: Show[Ast] = Show[Ast] {
    case ast: Query           => ast.show
    case ast: Function        => ast.show
    case ast: Value           => ast.show
    case ast: Operation       => ast.show
    case ast: Action          => ast.show
    case ast: Ident           => ast.show
    case ast: Property        => ast.show
    case ast: Infix           => ast.show
    case ast: OptionOperation => ast.show
    case ast: Dynamic         => ast.show
    case ast: If              => ast.show
  }

  implicit val ifShow: Show[If] = Show[If] {
    case If(a, b, c) => s"if($a) $b else $c"
  }

  implicit val dynamicShow: Show[Dynamic] = Show[Dynamic] {
    case Dynamic(tree) => tree.toString
  }

  implicit val queryShow: Show[Query] = Show[Query] {

    case q: Entity =>
      q.alias.map(a => s""""$a"""").toList ::: q.properties.map(p => s"""_.${p.property} -> "${p.alias}"""") match {
        case Nil    => s"query[${q.name}]"
        case params => s"query[${q.name}](${params.mkString(", ")})"
      }

    case Filter(source, alias, body) =>
      s"${source.show}.filter(${alias.show} => ${body.show})"

    case Map(source, alias, body) =>
      s"${source.show}.map(${alias.show} => ${body.show})"

    case FlatMap(source, alias, body) =>
      s"${source.show}.flatMap(${alias.show} => ${body.show})"

    case SortBy(source, alias, body, ordering) =>
      s"${source.show}.sortBy(${alias.show} => ${body.show})(${ordering.show})"

    case GroupBy(source, alias, body) =>
      s"${source.show}.groupBy(${alias.show} => ${body.show})"

    case Aggregation(op, ast) =>
      s"${scopedShow(ast)}.${op.show}"

    case Take(source, n) =>
      s"${source.show}.take($n)"

    case Drop(source, n) =>
      s"${source.show}.drop($n)"

    case Union(a, b) =>
      s"${a.show}.union(${b.show})"

    case UnionAll(a, b) =>
      s"${a.show}.unionAll(${b.show})"

    case Join(t, a, b, iA, iB, on) =>
      s"${a.show}.${t.show}(${b.show}).on((${iA.show}, ${iB.show}) => ${on.show})"
  }

  implicit val orderingShow: Show[Ordering] = Show[Ordering] {
    case TupleOrdering(elems) => s"Ord(${elems.show})"
    case Asc                  => s"Ord.asc"
    case Desc                 => s"Ord.desc"
    case AscNullsFirst        => s"Ord.ascNullsFirst"
    case DescNullsFirst       => s"Ord.descNullsFirst"
    case AscNullsLast         => s"Ord.ascNullsLast"
    case DescNullsLast        => s"Ord.descNullsLast"
  }

  implicit val optionOperationShow: Show[OptionOperation] = Show[OptionOperation] {
    case q: OptionOperation =>
      val method = q.t match {
        case OptionMap    => "map"
        case OptionForall => "forall"
        case OptionExists => "exists"
      }
      s"${q.ast}.$method((${q.alias.show}) => ${q.body.show})"
  }

  implicit val joinTypeShow: Show[JoinType] = Show[JoinType] {
    case InnerJoin => "join"
    case LeftJoin  => "leftJoin"
    case RightJoin => "rightJoin"
    case FullJoin  => "fullJoin"
  }

  implicit val functionShow: Show[Function] = Show[Function] {
    case Function(params, body) => s"(${params.show}) => ${body.show}"
  }

  implicit val operationShow: Show[Operation] = Show[Operation] {
    case UnaryOperation(op: PrefixUnaryOperator, ast)  => s"${op.show}${scopedShow(ast)}"
    case UnaryOperation(op: PostfixUnaryOperator, ast) => s"${scopedShow(ast)}.${op.show}"
    case BinaryOperation(a, op, b)                     => s"${scopedShow(a)} ${op.show} ${scopedShow(b)}"
    case FunctionApply(function, values)               => s"${scopedShow(function)}.apply(${values.show})"
  }

  implicit def operatorShow[T <: Operator]: Show[T] = Show[T] {
    case o => o.toString
  }

  implicit val propertyShow: Show[Property] = Show[Property] {
    case Property(ref, name) => s"${scopedShow(ref)}.$name"
  }

  implicit val valueShow: Show[Value] = Show[Value] {
    case Constant(v: String) => s""""$v""""
    case Constant(())        => s"{}"
    case Constant(v)         => s"$v"
    case NullValue           => s"null"
    case Tuple(values)       => s"(${values.show})"
    case Set(values)         => s"Set(${values.show})"
  }

  implicit val identShow: Show[Ident] = Show[Ident] {
    case e => e.name
  }

  implicit val actionShow: Show[Action] = Show[Action] {
    case AssignedAction(action, assignments) => s"${action.show}(${assignments.show})"
    case Update(query)                       => s"${query.show}.update"
    case Insert(query)                       => s"${query.show}.insert"
    case Delete(query)                       => s"${query.show}.delete"
  }

  implicit val assignmentShow: Show[Assignment] = Show[Assignment] {
    case Assignment(ident, property, value) => s"$ident => $ident.$property -> ${value.show}"
  }

  implicit val infixShow: Show[Infix] = Show[Infix] {
    case Infix(parts, params) =>
      def showParam(ast: Ast) =
        ast match {
          case ast: Ident => "$" + ast.show
          case other      => "$" + s"{${ast.show}}"
        }
      val ps = params.map(showParam)
      val body = StringContext(parts: _*).s(ps: _*)
      s"""infix"$body""""
  }

  private def scopedShow(ast: Ast) =
    ast match {
      case _: Function        => s"(${ast.show})"
      case _: BinaryOperation => s"(${ast.show})"
      case other              => ast.show
    }
}
