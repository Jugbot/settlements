package io.github.jugbot

import com.fasterxml.jackson.databind.JavaType
import io.github.jugbot.ai.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class BTSerialization extends AnyFunSuite with Matchers {
  private object ExampleBehavior {
    val EAT = "eat"
    val SLEEP = "sleep"
    val RAVE = "rave"
    val REPEAT = "repeat"
  }
  val jsonFixture: String = """{
                              |  sequence : [ "eat(food=bagel)", "repeat", {
                              |    selector : [ "sleep" ]
                              |  } ]
                              |}""".stripMargin

  val treeFixture: ParameterizedNode = SequenceNode(
    ActionNode(ExampleBehavior.EAT, Map("food" -> "bagel")),
    ActionNode(ExampleBehavior.REPEAT, Map.empty),
    SelectorNode(ActionNode(ExampleBehavior.SLEEP, Map.empty))
  )

  test("serializes enum to json") {
    val result: String = BTMapper.mapper.writeValueAsString(ActionNode(ExampleBehavior.EAT, Map("food" -> "bagel")))

    result shouldEqual """"eat(food=bagel)""""
  }

  test("deserializes json to enum") {
    val result = BTMapper.mapper.readValue("{eat: {food:\"bagel\"}}", classOf[Node[ParameterizedAction]])

    result shouldEqual ActionNode(ExampleBehavior.EAT, Map("food" -> "bagel"))
  }

  test("serializes to json") {
    val result = BTMapper.mapper.writeValueAsString(treeFixture)

    result shouldEqual jsonFixture
  }

  test("deserializes from json") {
    val result =
      BTMapper.mapper.readValue(jsonFixture, classOf[ParameterizedNode])

    result shouldEqual treeFixture
  }
}
