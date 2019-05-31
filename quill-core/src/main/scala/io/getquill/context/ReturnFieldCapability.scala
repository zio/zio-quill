package io.getquill.context

sealed trait InsertReturnCapability
trait ReturnSingleField extends InsertReturnCapability
trait ReturnMultipleField extends InsertReturnCapability

trait Capabilities {
  type ReturnAfterInsert <: InsertReturnCapability
}

trait CanReturnRecordAfterInsert extends Capabilities {
  override type ReturnAfterInsert = ReturnMultipleField
}
