package io.getquill.ast

sealed trait OptionOperationType

object OptionMap extends OptionOperationType
object OptionForall extends OptionOperationType