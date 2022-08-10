package io.getquill.jdbc

import java.text.{ DecimalFormat, DecimalFormatSymbols }
import java.util.Locale

object DataRenderer {

  def renderBoolean(value: Boolean) =
    if (value) "true"
    else "false"

  def string(value: String) =
    if (value == null) "null"
    else s"'${value.replaceAll("'", "''")}'"

  def double(value: Double) = {
    if (value.isNaN) "NaN"
    else if (value.isPosInfinity) "Infinity"
    else if (value.isNegInfinity) "-Infinity"
    // Need to cast in H2?
    else
      new DecimalFormat("0.################E0", DecimalFormatSymbols.getInstance(Locale.US)).format(value)
  }

  def float(value: Float) = {
    if (value.isNaN) "NaN"
    else if (value.isPosInfinity) "Infinity"
    else if (value.isNegInfinity) "-Infinity"
    // Need to cast in H2?
    else
      new DecimalFormat("0.#######E0", DecimalFormatSymbols.getInstance(Locale.US)).format(value)
  }
}
