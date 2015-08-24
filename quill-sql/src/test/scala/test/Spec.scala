package test

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FreeSpec
import org.scalatest.Inside
import org.scalatest.MustMatchers

trait Spec extends FreeSpec with MustMatchers with Inside with BeforeAndAfterAll
