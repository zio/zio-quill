package io.getquill.codegen.model

sealed trait UnrecognizedTypeStrategy
case object AssumeString extends UnrecognizedTypeStrategy
case object SkipColumn extends UnrecognizedTypeStrategy
case object ThrowTypingError extends UnrecognizedTypeStrategy

// TODO Need to document
sealed trait NumericPreference
case object PreferPrimitivesWhenPossible extends NumericPreference
case object UseDefaults extends NumericPreference

class TypingError(private val message: String) extends RuntimeException(message) {
}

