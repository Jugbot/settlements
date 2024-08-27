package io.github.jugbot.util

import io.github.jugbot.UnitSuite

class BooleanLogicParserSpec extends UnitSuite {

  def handleUnknownTag(tag: String): Boolean = throw MatchError(tag)

  test("parseAndEvaluate correctly parses and evaluates a simple expression") {
    val input = "A"
    val predicate: String => Boolean = {
      case "A" => true
      case tag => handleUnknownTag(tag)
    }
    BooleanLogicParser.parseAndEvaluate(input)(predicate) should be(true)
  }

  test("parseAndEvaluate correctly parses and evaluates an expression with 'and'") {
    val input = "A and B"
    val predicate: String => Boolean = {
      case "A" => true
      case "B" => true
      case tag => handleUnknownTag(tag)
    }
    BooleanLogicParser.parseAndEvaluate(input)(predicate) should be(true)
  }

  test("parseAndEvaluate correctly parses and evaluates an expression with 'or'") {
    val input = "A or B"
    val predicate: String => Boolean = {
      case "A" => false
      case "B" => true
      case tag => handleUnknownTag(tag)
    }
    BooleanLogicParser.parseAndEvaluate(input)(predicate) should be(true)
  }

  test("parseAndEvaluate correctly parses and evaluates an expression with 'not'") {
    val input = "not A"
    val predicate: String => Boolean = {
      case "A" => false
      case tag => handleUnknownTag(tag)
    }
    BooleanLogicParser.parseAndEvaluate(input)(predicate) should be(true)
  }

  test("parseAndEvaluate correctly parses and evaluates a complex expression") {
    val input = "A and B or (C and not D)"
    for {
      A <- Seq(true, false)
      B <- Seq(true, false)
      C <- Seq(true, false)
      D <- Seq(true, false)
    } {
      val predicate: String => Boolean = {
        case "A" => A
        case "B" => B
        case "C" => C
        case "D" => D
        case tag => handleUnknownTag(tag)
      }
      val expected = (A && B) || (C && !D)
      BooleanLogicParser.parseAndEvaluate(input)(predicate) should be(expected)
    }
  }

  test("parseAndEvaluate throws an exception for invalid input") {
    val input = "A and or B"
    val predicate: String => Boolean = _ => false
    an[IllegalArgumentException] should be thrownBy {
      BooleanLogicParser.parseAndEvaluate(input)
    }
  }
}
