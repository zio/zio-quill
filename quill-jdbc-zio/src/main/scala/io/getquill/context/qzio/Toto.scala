package io.getquill.context.qzio

import scala.language.reflectiveCalls;
scala.`package`.Nil.asInstanceOf[AnyRef{def size: Unit}].size;

val staticTopLevelQuat: io.getquill.quat.Quat.Product =
  io.getquill.quat.Quat.Product.WithRenamesCompact.apply(
    "Person",
    io.getquill.quat.Quat.Product.Type.Concrete
  )("name", "age")(io.getquill.quat.Quat.Value, io.getquill.quat.Quat.Value)()();

private[this] val x$12: (io.getquill.IdiomContext, io.getquill.context.Expand[io.getquill.PostgresZioJdbcContext[io.getquill.Literal.type]]) = ({
  val x$10: (io.getquill.PostgresDialect, io.getquill.Literal.type) =
    (
      scala.Tuple2.apply[
        io.getquill.PostgresDialect,
        io.getquill.Literal.type
      ](ctx.idiom, ctx.naming): (io.getquill.PostgresDialect, io.getquill.Literal.type) @unchecked
    ) match {
      case (
        _1: io.getquill.PostgresDialect,
        _2: io.getquill.Literal.type
      ): (io.getquill.PostgresDialect, io.getquill.Literal.type)(
        (idiom @ _), (naming @ _)
      ) => scala.Tuple2.apply[io.getquill.PostgresDialect, io.getquill.Literal.type](idiom, naming)
    };

  val idiom: io.getquill.PostgresDialect = x$10._1;
  val naming: io.getquill.Literal.type = x$10._2;

  val x$11: (io.getquill.ast.Entity, io.getquill.idiom.Statement, io.getquill.context.ExecutionType.Static.type, io.getquill.IdiomContext) =
    (
      scala.Tuple4.apply[
        io.getquill.ast.Entity,
        io.getquill.idiom.Statement,
        io.getquill.context.ExecutionType.Static.type,
        io.getquill.IdiomContext
      ](
        io.getquill.ast.Entity.Opinionated.apply("Person", scala.collection.immutable.Nil, io.getquill.quat.Quat.Product.WithRenamesCompact.apply("Person", io.getquill.quat.Quat.Product.Type.Concrete)("name", "age")(io.getquill.quat.Quat.Value, io.getquill.quat.Quat.Value)()(), io.getquill.ast.Renameable.ByStrategy),
        io.getquill.idiom.Statement.apply(scala.`package`.List.apply[io.getquill.idiom.StringToken](io.getquill.idiom.StringToken.apply("SELECT x.name, x.age FROM Person x"))),
        io.getquill.context.ExecutionType.Static,
        io.getquill.IdiomContext.apply(io.getquill.norm.TranspileConfig.apply(scala.collection.immutable.Nil, io.getquill.util.TraceConfig.apply(scala.collection.immutable.Nil)), io.getquill.IdiomContext.QueryType.Select)
      ): (io.getquill.ast.Entity, io.getquill.idiom.Statement, io.getquill.context.ExecutionType.Static.type, io.getquill.IdiomContext) @unchecked
    ) match {
      case (
        _1: io.getquill.ast.Entity,
        _2: io.getquill.idiom.Statement,
        _3: io.getquill.context.ExecutionType.Static.type,
        _4: io.getquill.IdiomContext
      ) => ((ast @ _), (statement @ _), (executionType @ _), (idiomContext @ _)) => scala.Tuple4.apply[io.getquill.ast.Entity, io.getquill.idiom.Statement, io.getquill.context.ExecutionType.Static.type, io.getquill.IdiomContext](ast, statement, executionType, idiomContext)
    };

  val ast: io.getquill.ast.Entity = x$11._1;
  val statement: io.getquill.idiom.Statement = x$11._2;
  val executionType: io.getquill.context.ExecutionType.Static.type = x$11._3;
  val idiomContext: io.getquill.IdiomContext = x$11._4;

  scala.Tuple2.apply[
    io.getquill.IdiomContext,
    io.getquill.context.Expand[io.getquill.PostgresZioJdbcContext[io.getquill.Literal.type]]
  ](
    idiomContext,
    io.getquill.context.Expand.apply[io.getquill.PostgresZioJdbcContext[io.getquill.Literal.type]](ctx, ast, statement, idiom, naming, executionType)
  )
}) match {
  case (
    _1: io.getquill.IdiomContext,
    _2: io.getquill.context.Expand[io.getquill.PostgresZioJdbcContext[io.getquill.Literal.type]]
    ): (io.getquill.IdiomContext, io.getquill.context.Expand[io.getquill.PostgresZioJdbcContext[io.getquill.Literal.type]])((idiomContext @ _), (expanded @ _)) => scala.Tuple2.apply[io.getquill.IdiomContext, io.getquill.context.Expand[io.getquill.PostgresZioJdbcContext[io.getquill.Literal.type]]](idiomContext, expanded)
};

val idiomContext: io.getquill.IdiomContext = x$12._1;
val expanded: io.getquill.context.Expand[io.getquill.PostgresZioJdbcContext[io.getquill.Literal.type]] = x$12._2;

ctx.streamQuery[ZioMockSpec.this.Person](
  scala.None,
  expanded.string,
  expanded.prepare,
  {
    final class $anon extends AnyRef with ctx.QueryMeta[ZioMockSpec.this.Person] {
      def <init>(): <$anon: ctx.QueryMeta[ZioMockSpec.this.Person]> = {
        $anon.super.<init>();
        ()
      };
      private[this] val _expanded: io.getquill.Quoted[io.getquill.Query[ZioMockSpec.this.Person] => io.getquill.Query[ZioMockSpec.this.Person]]{def quoted: io.getquill.ast.Function; def ast: io.getquill.ast.Function; def id974704609(): Unit; val liftings: Object} = {
        final class $anon extends AnyRef with io.getquill.Quoted[io.getquill.Query[ZioMockSpec.this.Person] => io.getquill.Query[ZioMockSpec.this.Person]] {
          def <init>(): <$anon: io.getquill.Quoted[io.getquill.Query[ZioMockSpec.this.Person] => io.getquill.Query[ZioMockSpec.this.Person]]> = {
            $anon.super.<init>();
            ()
          };
          import scala.language.reflectiveCalls;
          scala.`package`.Nil.asInstanceOf[AnyRef].size;
          def quoted: io.getquill.ast.Function = $anon.this.ast;
          override def ast: io.getquill.ast.Function =
            io.getquill.ast.Function.apply(
              scala.collection.immutable.List.apply[io.getquill.ast.Ident](
                io.getquill.ast.Ident.apply("q", io.getquill.quat.Quat.Product.WithRenamesCompact.apply("Person", io.getquill.quat.Quat.Product.Type.Concrete)("name", "age")(io.getquill.quat.Quat.Value, io.getquill.quat.Quat.Value)()())),
                io.getquill.ast.Map.apply(io.getquill.ast.Ident.apply("q", io.getquill.quat.Quat.Product.WithRenamesCompact.apply("Person", io.getquill.quat.Quat.Product.Type.Concrete)("name", "age")(io.getquill.quat.Quat.Value, io.getquill.quat.Quat.Value)()()),
                io.getquill.ast.Ident.apply("x", io.getquill.quat.Quat.Product.WithRenamesCompact.apply("Person", io.getquill.quat.Quat.Product.Type.Concrete)("name", "age")(io.getquill.quat.Quat.Value, io.getquill.quat.Quat.Value)()()),
                io.getquill.ast.Ident.apply("x", io.getquill.quat.Quat.Product.WithRenamesCompact.apply("Person", io.getquill.quat.Quat.Product.Type.Concrete)("name", "age")(io.getquill.quat.Quat.Value, io.getquill.quat.Quat.Value)()()))
              );
          def id974704609(): Unit = ();
          private[this] val liftings: Object = new scala.AnyRef();
          def liftings: Object = $anon.this.liftings
        };
        new $anon()
      };
      def expand: io.getquill.Quoted[io.getquill.Query[ZioMockSpec.this.Person] => io.getquill.Query[ZioMockSpec.this.Person]]{def quoted: io.getquill.ast.Function; def ast: io.getquill.ast.Function; def id974704609(): Unit; val liftings: Object} = $anon.this._expanded;
      private[this] val extract: (ctx.ResultRow, ctx.Session) => ZioMockSpec.this.Person = ((row: ctx.ResultRow, session: ctx.Session) => new ZioMockSpec.this.Person(scala.Predef.implicitly[ctx.Decoder[String]](ctx.stringDecoder).apply(0, row, session), scala.Predef.implicitly[ctx.Decoder[Int]](ctx.intDecoder).apply(1, row, session)));
      def extract: (ctx.ResultRow, ctx.Session) => ZioMockSpec.this.Person = $anon.this.extract
    };
    new $anon()
  }.extract
)(io.getquill.context.ExecutionInfo.apply(expanded.executionType, expanded.ast, staticTopLevelQuat), ())

