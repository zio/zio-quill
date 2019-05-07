package io.getquill.codegen.gen

trait QuerySchemaNaming {
  this: CodeGeneratorComponents =>

  object `"query"` extends QuerySchemaNaming {
    override def apply(tm: TableMeta): String = "query"
  }

  object `[namespace][Table]` extends QuerySchemaNaming {
    override def apply(tm: TableMeta): String =
      namespacer(tm) + tm.tableName.toLowerCase.capitalize
  }
}
