package io.getquill.idiom

import io.getquill.ast._
import io.getquill.util.Interleave
import io.getquill.util.Messages._

import scala.collection.mutable.ListBuffer

//noinspection ConvertExpressionToSAM
object StatementInterpolator {
  private[getquill] val emptyStatement: Statement               = stmt""
  private[getquill] val externalStatement: Statement            = stmt"?"
  private[getquill] val _externalTokenizer: Tokenizer[External] = Tokenizer[External](_ => externalStatement)

  trait Tokenizer[T] {
    def token(v: T): Token
  }

  object Tokenizer {
    def apply[T](f: T => Token): Tokenizer[T] =
      new Tokenizer[T] {
        def token(v: T): Token = f(v)
      }

    def withFallback[T](
      fallback: Tokenizer[T] => Tokenizer[T]
    )(pf: PartialFunction[T, Token]): Tokenizer[T] =
      new Tokenizer[T] {
        private lazy val stable: Tokenizer[T] = fallback(this)
        override def token(v: T): Token       = pf.applyOrElse(v, stable.token)
      }
  }

  implicit final class TokenImplicit[T](private val v: T) extends AnyVal {
    def token(implicit tokenizer: Tokenizer[T]): Token = tokenizer.token(v)
  }

  implicit val stringTokenizer: Tokenizer[String] =
    Tokenizer[String] { string =>
      StringToken(string)
    }

  implicit def externalTokenizer(implicit
    tagTokenizer: Tokenizer[Tag],
    liftTokenizer: Tokenizer[Lift]
  ): Tokenizer[External] =
    Tokenizer[External] {
      case tag: Tag   => tagTokenizer.token(tag)
      case lift: Lift => liftTokenizer.token(lift)
    }

  implicit val tagTokenizer: Tokenizer[Tag] =
    Tokenizer[Tag] {
      case tag: ScalarTag    => ScalarTagToken(tag)
      case tag: QuotationTag => QuotationTagToken(tag)
    }

  implicit val liftTokenizer: Tokenizer[Lift] =
    Tokenizer[Lift] {
      case lift: ScalarLift => ScalarLiftToken(lift)
      case lift: Lift =>
        fail(
          s"Can't tokenize a non-scalar lifting. ${lift.name}\n" +
            s"\n" +
            s"This might happen because:\n" +
            s"* You are trying to insert or update an `Option[A]` field, but Scala infers the type\n" +
            s"  to `Some[A]` or `None.type`. For example:\n" +
            s"    run(query[Users].update(_.optionalField -> lift(Some(value))))" +
            s"  In that case, make sure the type is `Option`:\n" +
            s"    run(query[Users].update(_.optionalField -> lift(Some(value): Option[Int])))\n" +
            s"  or\n" +
            s"    run(query[Users].update(_.optionalField -> lift(Option(value))))\n" +
            s"\n" +
            s"* You are trying to insert or update whole Embedded case class. For example:\n" +
            s"    run(query[Users].update(_.embeddedCaseClass -> lift(someInstance)))\n" +
            s"  In that case, make sure you are updating individual columns, for example:\n" +
            s"    run(query[Users].update(\n" +
            s"       _.embeddedCaseClass.a -> lift(someInstance.a),\n" +
            s"       _.embeddedCaseClass.b -> lift(someInstance.b)\n" +
            s"    ))" +
            s"\n" +
            s"* You are trying to insert or update an ADT field, but Scala infers the specific type\n" +
            s"  instead of the ADT type. For example:\n" +
            s"    case class User(role: UserRole)\n" +
            s"    sealed trait UserRole extends Product with Serializable\n" +
            s"    object UserRole {\n" +
            s"      case object Writer extends UserRole\n" +
            s"      case object Reader extends UserRole\n" +
            s"      implicit val encodeStatus: MappedEncoding[UserRole, String] = ...\n" +
            s"      implicit val decodeStatus: MappedEncoding[String, UserRole] = ...\n" +
            s"    }\n" +
            s"    run(query[User].update(_.role -> lift(UserRole.Writer)))\n" +
            s"  In that case, make sure you are uplifting to ADT type, for example:\n" +
            s"    run(query[User].update(_.role -> lift(UserRole.Writer: UserRole)))\n"
        )
    }

  implicit val tokenTokenizer: Tokenizer[Token] = Tokenizer[Token](identity)
  implicit val statementTokenizer: Tokenizer[Statement] =
    Tokenizer[Statement](identity)
  implicit val stringTokenTokenizer: Tokenizer[StringToken] =
    Tokenizer[StringToken](identity)
  implicit val liftingTokenTokenizer: Tokenizer[ScalarLiftToken] =
    Tokenizer[ScalarLiftToken](identity)

  implicit final class TokenList[T](private val list: List[T]) extends AnyVal {
    def mkStmt(sep: String = ", ")(implicit tokenize: Tokenizer[T]): Statement = {
      val l1 = list.map(_.token)
      val l2 = List.fill(l1.size - 1)(StringToken(sep))
      Statement(Interleave(l1, l2))
    }
  }

  implicit def listTokenizer[T](implicit tokenize: Tokenizer[T]): Tokenizer[List[T]] =
    Tokenizer[List[T]] { list =>
      list.mkStmt()
    }

  implicit final class Impl(private val sc: StringContext) extends AnyVal {

    private def flatten(tokens: List[Token]): List[Token] = {

      def unnestStatements(tokens: List[Token]): List[Token] =
        tokens.flatMap {
          case Statement(innerTokens) => unnestStatements(innerTokens)
          case token                  => token :: Nil
        }

      def mergeStringTokens(tokens: List[Token]): List[Token] = {
        val (resultBuilder, leftTokens) =
          tokens.foldLeft((new ListBuffer[Token], new ListBuffer[String])) {
            case ((builder, acc), stringToken: StringToken) =>
              val str = stringToken.string
              if (str.nonEmpty)
                acc += stringToken.string
              (builder, acc)
            case ((builder, prev), b) if prev.isEmpty =>
              (builder += b.token, prev)
            case ((builder, prev), b) /* if prev.nonEmpty */ =>
              builder += StringToken(prev.result().mkString)
              builder += b.token
              (builder, new ListBuffer[String])
          }
        if (leftTokens.nonEmpty)
          resultBuilder += StringToken(leftTokens.result().mkString)
        resultBuilder.result()
      }

      (unnestStatements _)
        .andThen(mergeStringTokens)
        .apply(tokens)
    }

    private def checkLengths(
      args: scala.collection.Seq[Any],
      parts: Seq[String]
    ): Unit =
      if (parts.length != args.length + 1)
        throw new IllegalArgumentException(
          "wrong number of arguments (" + args.length
            + ") for interpolated string with " + parts.length + " parts"
        )

    def stmt(args: Token*): Statement = {
      checkLengths(args, sc.parts)
      val partsIterator = sc.parts.iterator
      val argsIterator  = args.iterator
      val bldr          = List.newBuilder[Token]
      bldr += StringToken(partsIterator.next())
      while (argsIterator.hasNext) {
        bldr += argsIterator.next()
        bldr += StringToken(partsIterator.next())
      }
      val tokens = flatten(bldr.result())
      Statement(tokens)
    }
  }

}
