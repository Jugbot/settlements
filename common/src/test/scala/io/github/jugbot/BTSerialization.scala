package io.github.jugbot

import com.fasterxml.jackson.databind.JavaType
import io.github.jugbot.ai.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

enum ExampleBehavior(args: String*) {
  case unknown
  case eat(food: String) extends ExampleBehavior(food)
  case sleep
  case rave
  case re_eat(food: String) extends ExampleBehavior(food)
}

object ExampleBehavior {
  def valueOf(jsonValue: String): ExampleBehavior = {
    val tokens = jsonValue.split("""[\(\),]""").toList

    tokens match {
      case behaviorType :: args =>
        behaviorType match {
          case "eat"    => ExampleBehavior.eat(args(0))
          case "re_eat" => ExampleBehavior.re_eat(args(0))
          case "sleep"  => ExampleBehavior.sleep
          case "rave"   => ExampleBehavior.rave
          case _        => ExampleBehavior.unknown
        }
      case _ => ExampleBehavior.unknown
    }
  }
}

class BTSerialization extends AnyFunSuite with Matchers {
  val jsonFixture: String = """{
                              |  "sequence" : [ {
                              |    "action" : "eat(bagel)"
                              |  }, {
                              |    "action" : "re_eat(bagel)"
                              |  }, {
                              |    "selector" : [ {
                              |      "action" : "sleep"
                              |    } ]
                              |  } ]
                              |}""".stripMargin

  val treeFixture: SequenceNode[ExampleBehavior] = SequenceNode(
    ActionNode(ExampleBehavior.eat("bagel")),
    ActionNode(ExampleBehavior.re_eat("bagel")),
    SelectorNode(ActionNode(ExampleBehavior.sleep))
  )

  test("serializes enum to json") {
    val result: String = BTMapper.mapper.writeValueAsString(ExampleBehavior.eat("bagel"))

    println(result)

    result shouldEqual "\"eat(bagel)\""
  }

  test("deserializes unknown to enum") {
    val result = BTMapper.mapper.readValue("\"whatever\"", classOf[ExampleBehavior])

    println(result)

    result shouldEqual ExampleBehavior.unknown
  }

  test("deserializes json to enum") {
    val result = BTMapper.mapper.readValue("\"eat(bagel)\"", classOf[ExampleBehavior])

    println(result)

    result shouldEqual ExampleBehavior.eat("bagel")
  }

  test("serializes to json") {
    val result = BTMapper.mapper.writeValueAsString(treeFixture)

    println(result)

    result shouldEqual jsonFixture
  }

  test("deserializes from json") {
    val javaType: JavaType =
      BTMapper.mapper.getTypeFactory.constructType(classOf[ExampleBehavior])

    val typeRef = BTMapper.mapper.getTypeFactory
      .constructSimpleType(classOf[Node[?]], Array(javaType))
    println(typeRef.toString)
    val result: Node[ExampleBehavior] =
      BTMapper.mapper.readValue(jsonFixture, typeRef)

    println(result)

    result shouldEqual treeFixture
  }
}
