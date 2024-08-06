package io.github.jugbot

import com.fasterxml.jackson.databind.JavaType
import io.github.jugbot.ai.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import io.github.jugbot.meta.debugAST
import io.github.jugbot.meta.valueOf

sealed trait MyEnum
case class One() extends MyEnum
case class Two(value: String) extends MyEnum

class EnumUtilsTest extends UnitSuite {
  private def wrapper(arg: String, m: Map[String, String] = Map.empty) = valueOf[MyEnum](arg, m)
  test("valueOf with correct name returns some") {
    val myEnum = wrapper("One")
    myEnum shouldEqual Some(One())
  }
  test("valueOf without correct name returns none") {
    val myEnum = wrapper("OneTooMany")
    myEnum shouldEqual None
  }
  test("valueOf with correct name and args returns some") {
    val myEnum = wrapper("Two", Map("value" -> "A Value"))
    myEnum shouldEqual Some(Two("A Value"))
  }
  test("valueOf without correct name and args returns some") {
    val myEnum = wrapper("Two", Map.empty)
    myEnum shouldEqual None
  }
}
