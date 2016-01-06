package io.getquill.ast

sealed trait OptionOperationType

case object OptionMap extends OptionOperationType
case object OptionForall extends OptionOperationType
case object OptionExists extends OptionOperationType
