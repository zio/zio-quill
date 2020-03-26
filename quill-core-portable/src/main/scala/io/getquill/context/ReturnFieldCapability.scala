package io.getquill.context

sealed trait ReturningCapability

/**
 * Data cannot be returned Insert/Update/etc... clauses in the target database.
 */
sealed trait ReturningNotSupported extends ReturningCapability

/**
 * Returning a single field from Insert/Update/etc... clauses is supported. This is the most common
 * databases e.g. MySQL, Sqlite, and H2 (although as of h2database/h2database#1972 this may change. See #1496
 * regarding this. Typically this needs to be setup in the JDBC `connection.prepareStatement(sql, Array("returnColumn"))`.
 */
sealed trait ReturningSingleFieldSupported extends ReturningCapability

/**
 * Returning multiple columns from Insert/Update/etc... clauses is supported. This generally means that
 * columns besides auto-incrementing ones can be returned. This is supported by Oracle.
 * In JDBC, the following is done:
 * `connection.prepareStatement(sql, Array("column1, column2, ..."))`.
 */
sealed trait ReturningMultipleFieldSupported extends ReturningCapability

/**
 * An actual `RETURNING` clause is supported in the SQL dialect of the specified database e.g. Postgres.
 * this typically means that columns returned from Insert/Update/etc... clauses can have other database
 * operations done on them such as arithmetic `RETURNING id + 1`, UDFs `RETURNING udf(id)` or others.
 * In JDBC, the following is done:
 * `connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))`.
 */
sealed trait ReturningClauseSupported extends ReturningCapability

/**
 * An actual `OUTPUT` clause is supported in the SQL dialect of the specified database e.g. MSSQL Server.
 * this typically means that columns returned from Insert/Update/etc... clauses can have arithmetic
 * operations done on them such as `OUTPUT INSERTED.id + 1`.
 */
sealed trait OutputClauseSupported extends ReturningCapability

object ReturningNotSupported extends ReturningNotSupported
object ReturningSingleFieldSupported extends ReturningSingleFieldSupported
object ReturningMultipleFieldSupported extends ReturningMultipleFieldSupported
object ReturningClauseSupported extends ReturningClauseSupported
object OutputClauseSupported extends OutputClauseSupported

trait Capabilities {
  def idiomReturningCapability: ReturningCapability
}

trait CanReturnClause extends Capabilities {
  override def idiomReturningCapability: ReturningClauseSupported = ReturningClauseSupported
}

trait CanOutputClause extends Capabilities {
  override def idiomReturningCapability: OutputClauseSupported = OutputClauseSupported
}

trait CanReturnField extends Capabilities {
  override def idiomReturningCapability: ReturningSingleFieldSupported = ReturningSingleFieldSupported
}

trait CanReturnMultiField extends Capabilities {
  override def idiomReturningCapability: ReturningMultipleFieldSupported = ReturningMultipleFieldSupported
}

trait CannotReturn extends Capabilities {
  override def idiomReturningCapability: ReturningNotSupported = ReturningNotSupported
}
