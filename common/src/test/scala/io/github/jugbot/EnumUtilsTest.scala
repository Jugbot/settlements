package io.github.jugbot

import io.github.jugbot.ai.*

sealed trait MyEnum
case class One() extends MyEnum
case object Three extends MyEnum

sealed trait MyEnumTwo extends MyEnum
case class Two(value: String) extends MyEnumTwo


class EnumUtilsTest extends UnitSuite {
  private def wrapper(arg: String, m: Map[String, String] = Map.empty) = meta.valueOf[MyEnum](arg, m)
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
  test("valueOf with correct name of object returns some") {
    val myEnum = wrapper("Three")
    myEnum shouldEqual Some(Three)
  }
}
