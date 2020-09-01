package io.getquill

import io.getquill.ast.Renameable.{ ByStrategy, Fixed }
import io.getquill.ast.Visibility.Hidden
import io.getquill.ast.{ Query => AstQuery, Action => AstAction, _ }
import io.getquill.context.CanReturnClause
import io.getquill.idiom.{ Idiom, SetContainsToken, Statement }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.norm.Normalize
import io.getquill.util.Interleave

object MirrorIdiom extends MirrorIdiom
class MirrorIdiom extends MirrorIdiomBase with CanReturnClause

object MirrorIdiomPrinting extends MirrorIdiom {
  override def distinguishHidden: Boolean = true
}

trait MirrorIdiomBase extends Idiom {

  def distinguishHidden: Boolean = false

  override def prepareForProbing(string: String) = string

  override def liftingPlaceholder(index: Int): String = "?"

  override def translate(ast: Ast)(implicit naming: NamingStrategy): (Ast, Statement) = {
    val normalizedAst = Normalize(ast)
    (normalizedAst, stmt"${normalizedAst.token}")
  }

  implicit def astTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[Ast] = Tokenizer[Ast] {
    case ast: AstAction           => ast.token
    case ast: Function            => ast.token
    case ast: Value               => ast.token
    case ast: Operation           => ast.token
    case ast: AstQuery            => ast.token
    case ast: Ident               => ast.token
    case ast: ExternalIdent       => ast.token
    case ast: Property            => ast.token
    case ast: Infix               => ast.token
    case ast: OptionOperation     => ast.token
    case ast: IterableOperation   => ast.token
    case ast: Dynamic             => ast.token
    case ast: If                  => ast.token
    case ast: Block               => ast.token
    case ast: Val                 => ast.token
    case ast: Ordering            => ast.token
    case ast: QuotedReference     => ast.ast.token
    case ast: External            => ast.token
    case ast: Assignment          => ast.token
    case ast: OnConflict.Excluded => ast.token
    case ast: OnConflict.Existing => ast.token
  }

  implicit def ifTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[If] = Tokenizer[If] {
    case If(a, b, c) => stmt"if(${a.token}) ${b.token} else ${c.token}"
  }

  implicit val dynamicTokenizer: Tokenizer[Dynamic] = Tokenizer[Dynamic] {
    case Dynamic(tree, _) => stmt"${tree.toString.token}"
  }

  implicit def blockTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[Block] = Tokenizer[Block] {
    case Block(statements) => stmt"{ ${statements.map(_.token).mkStmt("; ")} }"
  }

  implicit def valTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[Val] = Tokenizer[Val] {
    case Val(name, body) => stmt"val ${name.token} = ${body.token}"
  }

  implicit def queryTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[AstQuery] = Tokenizer[AstQuery] {

    case Entity.Opinionated(name, Nil, _, renameable) => stmt"${tokenizeName("querySchema", renameable).token}(${s""""$name"""".token})"

    case Entity.Opinionated(name, prop, _, renameable) =>
      val properties = prop.map(p => stmt"""_.${p.path.mkStmt(".")} -> "${p.alias.token}"""")
      stmt"${tokenizeName("querySchema", renameable).token}(${s""""$name"""".token}, ${properties.token})"

    case Filter(source, alias, body) =>
      stmt"${source.token}.filter(${alias.token} => ${body.token})"

    case Map(source, alias, body) =>
      stmt"${source.token}.map(${alias.token} => ${body.token})"

    case FlatMap(source, alias, body) =>
      stmt"${source.token}.flatMap(${alias.token} => ${body.token})"

    case ConcatMap(source, alias, body) =>
      stmt"${source.token}.concatMap(${alias.token} => ${body.token})"

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

  implicit def optionOperationTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[OptionOperation] = Tokenizer[OptionOperation] {
    case OptionTableFlatMap(ast, alias, body) => stmt"${ast.token}.flatMap((${alias.token}) => ${body.token})"
    case OptionTableMap(ast, alias, body)     => stmt"${ast.token}.map((${alias.token}) => ${body.token})"
    case OptionTableExists(ast, alias, body)  => stmt"${ast.token}.exists((${alias.token}) => ${body.token})"
    case OptionTableForall(ast, alias, body)  => stmt"${ast.token}.forall((${alias.token}) => ${body.token})"
    case OptionFlatten(ast)                   => stmt"${ast.token}.flatten"
    case OptionGetOrElse(ast, body)           => stmt"${ast.token}.getOrElse(${body.token})"
    case OptionFlatMap(ast, alias, body)      => stmt"${ast.token}.flatMap((${alias.token}) => ${body.token})"
    case OptionMap(ast, alias, body)          => stmt"${ast.token}.map((${alias.token}) => ${body.token})"
    case OptionForall(ast, alias, body)       => stmt"${ast.token}.forall((${alias.token}) => ${body.token})"
    case OptionExists(ast, alias, body)       => stmt"${ast.token}.exists((${alias.token}) => ${body.token})"
    case OptionContains(ast, body)            => stmt"${ast.token}.contains(${body.token})"
    case OptionIsEmpty(ast)                   => stmt"${ast.token}.isEmpty"
    case OptionNonEmpty(ast)                  => stmt"${ast.token}.nonEmpty"
    case OptionIsDefined(ast)                 => stmt"${ast.token}.isDefined"
    case OptionSome(ast)                      => stmt"Some(${ast.token})"
    case OptionApply(ast)                     => stmt"Option(${ast.token})"
    case OptionOrNull(ast)                    => stmt"${ast.token}.orNull"
    case OptionGetOrNull(ast)                 => stmt"${ast.token}.getOrNull"
    case OptionNone(_)                        => stmt"None"
  }

  implicit def traversableOperationTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[IterableOperation] = Tokenizer[IterableOperation] {
    case MapContains(ast, body)  => stmt"${ast.token}.contains(${body.token})"
    case SetContains(ast, body)  => stmt"${ast.token}.contains(${body.token})"
    case ListContains(ast, body) => stmt"${ast.token}.contains(${body.token})"
  }

  implicit val joinTypeTokenizer: Tokenizer[JoinType] = Tokenizer[JoinType] {
    case InnerJoin => stmt"join"
    case LeftJoin  => stmt"leftJoin"
    case RightJoin => stmt"rightJoin"
    case FullJoin  => stmt"fullJoin"
  }

  implicit def functionTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[Function] = Tokenizer[Function] {
    case Function(params, body) => stmt"(${params.token}) => ${body.token}"
  }

  implicit def operationTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[Operation] = Tokenizer[Operation] {
    case UnaryOperation(op: PrefixUnaryOperator, ast)       => stmt"${op.token}${scopedTokenizer(ast)}"
    case UnaryOperation(op: PostfixUnaryOperator, ast)      => stmt"${scopedTokenizer(ast)}.${op.token}"
    case BinaryOperation(a, op @ SetOperator.`contains`, b) => SetContainsToken(scopedTokenizer(b), op.token, a.token)
    case BinaryOperation(a, op, b)                          => stmt"${scopedTokenizer(a)} ${op.token} ${scopedTokenizer(b)}"
    case FunctionApply(function, values)                    => stmt"${scopedTokenizer(function)}.apply(${values.token})"
  }

  implicit def operatorTokenizer[T <: Operator]: Tokenizer[T] = Tokenizer[T] {
    case o => stmt"${o.toString.token}"
  }

  def tokenizeName(name: String, renameable: Renameable) =
    renameable match {
      case ByStrategy => name
      case Fixed      => s"`${name}`"
    }

  def bracketIfHidden(name: String, visibility: Visibility) =
    (distinguishHidden, visibility) match {
      case (true, Hidden) => s"[$name]"
      case _              => name
    }

  implicit def propertyTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[Property] = Tokenizer[Property] {
    case Property.Opinionated(ExternalIdent(_, _), name, renameable, visibility) => stmt"${bracketIfHidden(tokenizeName(name, renameable), visibility).token}"
    case Property.Opinionated(ref, name, renameable, visibility)                 => stmt"${scopedTokenizer(ref)}.${bracketIfHidden(tokenizeName(name, renameable), visibility).token}"
  }

  implicit val valueTokenizer: Tokenizer[Value] = Tokenizer[Value] {
    case Constant(v: String, _) => stmt""""${v.token}""""
    case Constant((), _)        => stmt"{}"
    case Constant(v, _)         => stmt"${v.toString.token}"
    case NullValue              => stmt"null"
    case Tuple(values)          => stmt"(${values.token})"
    case CaseClass(values)      => stmt"CaseClass(${values.map { case (k, v) => s"${k.token}: ${v.token}" }.mkString(", ").token})"
  }

  implicit val identTokenizer: Tokenizer[Ident] = Tokenizer[Ident] {
    case Ident.Opinionated(name, quat, visibility) => stmt"${bracketIfHidden(name, visibility).token}"
  }

  implicit val typeTokenizer: Tokenizer[ExternalIdent] = Tokenizer[ExternalIdent] {
    case e => stmt"${e.name.token}"
  }

  implicit val excludedTokenizer: Tokenizer[OnConflict.Excluded] = Tokenizer[OnConflict.Excluded] {
    case OnConflict.Excluded(ident) => stmt"${ident.token}"
  }

  implicit val existingTokenizer: Tokenizer[OnConflict.Existing] = Tokenizer[OnConflict.Existing] {
    case OnConflict.Existing(ident) => stmt"${ident.token}"
  }

  implicit def actionTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[AstAction] = Tokenizer[AstAction] {
    case Update(query, assignments)             => stmt"${query.token}.update(${assignments.token})"
    case Insert(query, assignments)             => stmt"${query.token}.insert(${assignments.token})"
    case Delete(query)                          => stmt"${query.token}.delete"
    case Returning(query, alias, body)          => stmt"${query.token}.returning((${alias.token}) => ${body.token})"
    case ReturningGenerated(query, alias, body) => stmt"${query.token}.returningGenerated((${alias.token}) => ${body.token})"
    case Foreach(query, alias, body)            => stmt"${query.token}.foreach((${alias.token}) => ${body.token})"
    case c: OnConflict                          => stmt"${c.token}"
  }

  implicit def conflictTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[OnConflict] = {

    def targetProps(l: List[Property]) = l.map(p => Transform(p) {
      case Ident(_, quat) => Ident("_", quat)
    })

    implicit val conflictTargetTokenizer = Tokenizer[OnConflict.Target] {
      case OnConflict.NoTarget          => stmt""
      case OnConflict.Properties(props) => stmt"(${targetProps(props).token})"
    }

    val updateAssignsTokenizer = Tokenizer[Assignment] {
      case Assignment(i, p, v) =>
        stmt"(${i.token}, e) => ${p.token} -> ${scopedTokenizer(v)}"
    }

    Tokenizer[OnConflict] {
      case OnConflict(i, t, OnConflict.Update(assign)) =>
        stmt"${i.token}.onConflictUpdate${t.token}(${assign.map(updateAssignsTokenizer.token).mkStmt()})"
      case OnConflict(i, t, OnConflict.Ignore) =>
        stmt"${i.token}.onConflictIgnore${t.token}"
    }
  }

  implicit def assignmentTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[Assignment] = Tokenizer[Assignment] {
    case Assignment(ident, property, value) => stmt"${ident.token} => ${property.token} -> ${value.token}"
  }

  implicit def infixTokenizer(implicit externalTokenizer: Tokenizer[External]): Tokenizer[Infix] = Tokenizer[Infix] {
    case Infix(parts, params, _, _) =>
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

  private def scopedTokenizer(ast: Ast)(implicit externalTokenizer: Tokenizer[External]) =
    ast match {
      case _: Function        => stmt"(${ast.token})"
      case _: BinaryOperation => stmt"(${ast.token})"
      case other              => ast.token
    }
}
