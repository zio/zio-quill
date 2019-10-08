package io.getquill.idiom

import io.getquill.ast._
import io.getquill.util.Interleave
import io.getquill.util.Messages._

import scala.collection.mutable.ListBuffer

object StatementInterpolator {

  trait Tokenizer[T] {
    def token(v: T): Token
  }

  object Tokenizer {
    def apply[T](f: T => Token) = new Tokenizer[T] {
      def token(v: T) = f(v)
    }
    def withFallback[T](fallback: Tokenizer[T] => Tokenizer[T])(pf: PartialFunction[T, Token]) =
      new Tokenizer[T] {
        private val stable = fallback(this)
        override def token(v: T) = pf.applyOrElse(v, stable.token)
      }
  }

  implicit class TokenImplicit[T](v: T)(implicit tokenizer: Tokenizer[T]) {
    def token = tokenizer.token(v)
  }

  implicit def stringTokenizer: Tokenizer[String] =
    Tokenizer[String] {
      case string => StringToken(string)
    }

  implicit def liftTokenizer: Tokenizer[Lift] =
    Tokenizer[Lift] {
      case lift: ScalarLift => ScalarLiftToken(lift)
      case lift => fail(
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
          s"    ))"
      )
    }

  implicit def tokenTokenizer: Tokenizer[Token] = Tokenizer[Token](identity)
  implicit def statementTokenizer: Tokenizer[Statement] = Tokenizer[Statement](identity)
  implicit def stringTokenTokenizer: Tokenizer[StringToken] = Tokenizer[StringToken](identity)
  implicit def liftingTokenTokenizer: Tokenizer[ScalarLiftToken] = Tokenizer[ScalarLiftToken](identity)

  implicit class TokenList[T](list: List[T]) {
    def mkStmt(sep: String = ", ")(implicit tokenize: Tokenizer[T]) = {
      val l1 = list.map(_.token)
      val l2 = List.fill(l1.size - 1)(StringToken(sep))
      Statement(Interleave(l1, l2))
    }
  }

  implicit def listTokenizer[T](implicit tokenize: Tokenizer[T]): Tokenizer[List[T]] =
    Tokenizer[List[T]] {
      case list => list.mkStmt()
    }

  implicit class Impl(sc: StringContext) {

    private def flatten(tokens: List[Token]): List[Token] = {

      def unestStatements(tokens: List[Token]): List[Token] = {
        tokens.flatMap {
          case Statement(innerTokens) => unestStatements(innerTokens)
          case token                  => token :: Nil
        }
      }

      def mergeStringTokens(tokens: List[Token]): List[Token] = {
        val (resultBuilder, leftTokens) = tokens.foldLeft((new ListBuffer[Token], new ListBuffer[String])) {
          case ((builder, acc), stringToken: StringToken) =>
            val str = stringToken.string
            if (str.nonEmpty)
              acc += stringToken.string
            (builder, acc)
          case ((builder, prev), b) if prev.isEmpty => (builder += b.token, prev)
          case ((builder, prev), b) /* if prev.nonEmpty */ =>
            builder += StringToken(prev.result().mkString)
            builder += b.token
            (builder, new ListBuffer[String])
        }
        if (leftTokens.nonEmpty)
          resultBuilder += StringToken(leftTokens.result().mkString)
        resultBuilder.result()
      }

      (unestStatements _)
        .andThen(mergeStringTokens _)
        .apply(tokens)
    }

    private def checkLengths(args: scala.collection.Seq[Any], parts: Seq[String]): Unit =
      if (parts.length != args.length + 1)
        throw new IllegalArgumentException("wrong number of arguments (" + args.length
          + ") for interpolated string with " + parts.length + " parts")

    def stmt(args: Token*): Statement = {
      checkLengths(args, sc.parts)
      val partsIterator = sc.parts.iterator
      val argsIterator = args.iterator
      val bldr = List.newBuilder[Token]
      bldr += StringToken(partsIterator.next())
      while (argsIterator.hasNext) {
        bldr += argsIterator.next
        bldr += StringToken(partsIterator.next())
      }
      val tokens = flatten(bldr.result)
      Statement(tokens)
    }
  }
}
