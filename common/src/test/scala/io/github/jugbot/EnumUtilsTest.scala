package io.github.jugbot

import com.fasterxml.jackson.databind.JavaType
import io.github.jugbot.ai.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import io.github.jugbot.util.debugAST
import io.github.jugbot.util.valueOf

sealed trait MyEnum
case class One() extends MyEnum
case class Two() extends MyEnum

class EnumUtilsTest extends AnyFunSuite with Matchers {
  def wrapper(arg: String) = valueOf[MyEnum](arg, Map.empty)
  test("valueOf") {
    val myEnum = wrapper("One").get
    myEnum shouldEqual One()
  }
}
