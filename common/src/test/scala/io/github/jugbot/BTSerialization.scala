package io.github.jugbot

import com.fasterxml.jackson.databind.JavaType
import io.github.jugbot.ai.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class BehaviorTreeSerialization extends UnitSuite {
  val jsonFixture: String = """{
                              |  sequence : [ "eat(food=bagel)", "repeat", {
                              |    selector : [ "sleep" ]
                              |  } ]
                              |}""".stripMargin

  val treeFixture: ParameterizedNode = SequenceNode(
    ActionNode("eat", Map("food" -> "bagel")),
    ActionNode("repeat", Map.empty),
    SelectorNode(ActionNode("sleep", Map.empty))
  )

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

class ActionNodeSerialization extends UnitSuite {
  val node = ActionNode("eat", Map("food" -> "bagel"))
  val jsonShorthand = """"eat(food=bagel)""""
  val json = """{eat: {food:"bagel"}}"""

  test("serializes node to json shorthand") {
    val result: String = BTMapper.mapper.writeValueAsString(node)

    result shouldEqual jsonShorthand
  }

  test("deserializes json to node") {
    val result = BTMapper.mapper.readValue(json, classOf[Node[ParameterizedAction]])

    result shouldEqual node
  }

  test("deserializes json shorthand to node") {
    val result = BTMapper.mapper.readValue(jsonShorthand, classOf[Node[ParameterizedAction]])

    result shouldEqual node
  }
}

class ConditionalNodeSerialization extends UnitSuite {
  val node = ConditionNode(ActionNode("eat", Map()), ActionNode("sleep", Map()), ActionNode("rave", Map()))
  val json = """{condition: {if:"eat", then:"sleep", else: "rave"}}"""

  ignore("serializes node to json") {
    val result: String = BTMapper.mapper.writeValueAsString(node)

    result shouldEqual json
  }

  test("deserializes json to node") {
    val result = BTMapper.mapper.readValue(json, classOf[Node[ParameterizedAction]])

    result shouldEqual node
  }
}

class SequenceNodeSerialization extends UnitSuite {
  val node = SequenceNode(ActionNode("eat", Map()), ActionNode("sleep", Map()), ActionNode("rave", Map()))
  val json = """{
               |  sequence : [ "eat", "sleep", "rave" ]
               |}""".stripMargin

  test("serializes node to json") {
    val result: String = BTMapper.mapper.writeValueAsString(node)

    result shouldEqual json
  }

  test("deserializes json to node") {
    val result = BTMapper.mapper.readValue(json, classOf[Node[ParameterizedAction]])

    result shouldEqual node
  }
}

class SelectorNodeSerialization extends UnitSuite {
  val node = SelectorNode(ActionNode("eat", Map()), ActionNode("sleep", Map()), ActionNode("rave", Map()))
  val json = """{
               |  selector : [ "eat", "sleep", "rave" ]
               |}""".stripMargin

  test("serializes node to json") {
    val result: String = BTMapper.mapper.writeValueAsString(node)

    result shouldEqual json
  }

  test("deserializes json to node") {
    val result = BTMapper.mapper.readValue(json, classOf[Node[ParameterizedAction]])

    result shouldEqual node
  }
}
