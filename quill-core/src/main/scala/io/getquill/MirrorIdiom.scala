package io.getquill

import io.getquill.ast._
import io.getquill.idiom.Idiom
import io.getquill.idiom.SetContainsToken
import io.getquill.idiom.Statement
import io.getquill.idiom.StatementInterpolator._
import io.getquill.norm.Normalize
import io.getquill.util.Interleave

object MirrorIdiom extends MirrorIdiom

class MirrorIdiom extends Idiom {

  override def prepareForProbing(string: String) = string

  override def liftingPlaceholder(index: Int): String = "?"

  override def translate(ast: Ast)(implicit naming: NamingStrategy): (Ast, Statement) = {
    val normalizedAst = Normalize(ast)
    (normalizedAst, stmt"${normalizedAst.token}")
  }

  implicit def astTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Ast] = Tokenizer[Ast] {
    case ast: Query           => ast.token
    case ast: Function        => ast.token
    case ast: Value           => ast.token
    case ast: Operation       => ast.token
    case ast: Action          => ast.token
    case ast: Ident           => ast.token
    case ast: Property        => ast.token
    case ast: Infix           => ast.token
    case ast: OptionOperation => ast.token
    case ast: Dynamic         => ast.token
    case ast: If              => ast.token
    case ast: Block           => ast.token
    case ast: Val             => ast.token
    case ast: Ordering        => ast.token
    case ast: QuotedReference => ast.ast.token
    case ast: Lift            => ast.token
    case ast: Assignment      => ast.token
  }

  implicit def ifTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[If] = Tokenizer[If] {
    case If(a, b, c) => stmt"if(${a.token}) ${b.token} else ${c.token}"
  }

  implicit val dynamicTokenizer: Tokenizer[Dynamic] = Tokenizer[Dynamic] {
    case Dynamic(tree) => stmt"${tree.toString.token}"
  }

  implicit def blockTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Block] = Tokenizer[Block] {
    case Block(statements) => stmt"{ ${statements.map(_.token).mkStmt("; ")} }"
  }

  implicit def valTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Val] = Tokenizer[Val] {
    case Val(name, body) => stmt"val ${name.token} = ${body.token}"
  }

  implicit def queryTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Query] = Tokenizer[Query] {

    case Entity(name, Nil) => stmt"querySchema(${s""""$name"""".token})"

    case Entity(name, prop) =>
      val properties = prop.map(p => stmt"""_.${p.path.mkStmt(".")} -> "${p.alias.token}"""")
      stmt"querySchema(${s""""$name"""".token}, ${properties.token})"

    case Filter(source, alias, body) =>
      stmt"${source.token}.filter(${alias.token} => ${body.token})"

    case Map(source, alias, body) =>
      stmt"${source.token}.map(${alias.token} => ${body.token})"

    case FlatMap(source, alias, body) =>
      stmt"${source.token}.flatMap(${alias.token} => ${body.token})"

    case SortBy(source, alias, body, ordering) =>
      stmt"${source.token}.sortBy(${alias.token} => ${body.token})(${ordering.token})"

    case GroupBy(source, alias, body) =>
      stmt"${source.token}.groupBy(${alias.token} => ${body.token})"

    case Aggregation(op, ast) =>
      stmt"${scopedTokenizer(ast)}.${op.token}"

    case Take(source, n) =>
      stmt"${source.token}.take(${n.token})"

    case Drop(source, n) =>
      stmt"${source.token}.drop(${n.token})"

    case Union(a, b) =>
      stmt"${a.token}.union(${b.token})"

    case UnionAll(a, b) =>
      stmt"${a.token}.unionAll(${b.token})"

    case Join(t, a, b, iA, iB, on) =>
      stmt"${a.token}.${t.token}(${b.token}).on((${iA.token}, ${iB.token}) => ${on.token})"

    case FlatJoin(t, a, iA, on) =>
      stmt"${a.token}.${t.token}((${iA.token}) => ${on.token})"

    case Distinct(a) =>
      stmt"${a.token}.distinct"

    case Nested(a) =>
      stmt"${a.token}.nested"
  }

  implicit val orderingTokenizer: Tokenizer[Ordering] = Tokenizer[Ordering] {
    case TupleOrdering(elems) => stmt"Ord(${elems.token})"
    case Asc                  => stmt"Ord.asc"
    case Desc                 => stmt"Ord.desc"
    case AscNullsFirst        => stmt"Ord.ascNullsFirst"
    case DescNullsFirst       => stmt"Ord.descNullsFirst"
    case AscNullsLast         => stmt"Ord.ascNullsLast"
    case DescNullsLast        => stmt"Ord.descNullsLast"
  }

  implicit def optionOperationTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[OptionOperation] = Tokenizer[OptionOperation] {
    case OptionMap(ast, alias, body)    => stmt"${ast.token}.map((${alias.token}) => ${body.token})"
    case OptionForall(ast, alias, body) => stmt"${ast.token}.forall((${alias.token}) => ${body.token})"
    case OptionExists(ast, alias, body) => stmt"${ast.token}.exists((${alias.token}) => ${body.token})"
    case OptionContains(ast, body)      => stmt"${ast.token}.contains(${body.token})"
  }

  implicit val joinTypeTokenizer: Tokenizer[JoinType] = Tokenizer[JoinType] {
    case InnerJoin => stmt"join"
    case LeftJoin  => stmt"leftJoin"
    case RightJoin => stmt"rightJoin"
    case FullJoin  => stmt"fullJoin"
  }

  implicit def functionTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Function] = Tokenizer[Function] {
    case Function(params, body) => stmt"(${params.token}) => ${body.token}"
  }

  implicit def operationTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Operation] = Tokenizer[Operation] {
    case UnaryOperation(op: PrefixUnaryOperator, ast)       => stmt"${op.token}${scopedTokenizer(ast)}"
    case UnaryOperation(op: PostfixUnaryOperator, ast)      => stmt"${scopedTokenizer(ast)}.${op.token}"
    case BinaryOperation(a, op @ SetOperator.`contains`, b) => SetContainsToken(scopedTokenizer(b), op.token, a.token)
    case BinaryOperation(a, op, b)                          => stmt"${scopedTokenizer(a)} ${op.token} ${scopedTokenizer(b)}"
    case FunctionApply(function, values)                    => stmt"${scopedTokenizer(function)}.apply(${values.token})"
  }

  implicit def operatorTokenizer[T <: Operator]: Tokenizer[T] = Tokenizer[T] {
    case o => stmt"${o.toString.token}"
  }

  implicit def propertyTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Property] = Tokenizer[Property] {
    case Property(ref, name) => stmt"${scopedTokenizer(ref)}.${name.token}"
  }

  implicit val valueTokenizer: Tokenizer[Value] = Tokenizer[Value] {
    case Constant(v: String) => stmt""""${v.token}""""
    case Constant(())        => stmt"{}"
    case Constant(v)         => stmt"${v.toString.token}"
    case NullValue           => stmt"null"
    case Tuple(values)       => stmt"(${values.token})"
  }

  implicit val identTokenizer: Tokenizer[Ident] = Tokenizer[Ident] {
    case e => stmt"${e.name.token}"
  }

  implicit def actionTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Action] = Tokenizer[Action] {
    case Update(query, assignments)    => stmt"${query.token}.update(${assignments.token})"
    case Insert(query, assignments)    => stmt"${query.token}.insert(${assignments.token})"
    case Delete(query)                 => stmt"${query.token}.delete"
    case Returning(query, alias, body) => stmt"${query.token}.returning((${alias.token}) => ${body.token})"
    case Foreach(query, alias, body)   => stmt"${query.token}.foreach((${alias.token}) => ${body.token})"
  }

  implicit def assignmentTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Assignment] = Tokenizer[Assignment] {
    case Assignment(ident, property, value) => stmt"${ident.token} => ${property.token} -> ${value.token}"
  }

  implicit def infixTokenizer(implicit liftTokenizer: Tokenizer[Lift]): Tokenizer[Infix] = Tokenizer[Infix] {
    case Infix(parts, params) =>
      def tokenParam(ast: Ast) =
        ast match {
          case ast: Ident => stmt"$$${ast.token}"
          case other      => stmt"$${${ast.token}}"
        }

      val pt = parts.map(_.token)
      val pr = params.map(tokenParam)
      val body = Statement(Interleave(pt, pr))
      stmt"""infix"${body.token}""""
  }

  private def scopedTokenizer(ast: Ast)(implicit liftTokenizer: Tokenizer[Lift]) =
    ast match {
      case _: Function        => stmt"(${ast.token})"
      case _: BinaryOperation => stmt"(${ast.token})"
      case other              => ast.token
    }
}
