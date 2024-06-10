package io.github.jugbot

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import io.github.jugbot.ai.{
  SelectorNode,
  state,
  Failure,
  ActionNode,
  SequenceNode,
  Success,
  Status
}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.module.scala.EnumModule
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.BTModule
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.annotation.JsonFormat.Feature
import com.fasterxml.jackson.core.json.JsonReadFeature
import io.github.jugbot.ai.BTMapper

class BTSerialization extends AnyFunSuite with Matchers {
  val jsonFixture = """{
                    |  "sequence" : [ {
                    |    "action" : "EAT"
                    |  }, {
                    |    "action" : "RE_EAT"
                    |  }, {
                    |    "selector" : [ {
                    |      "action" : "SLEEP"
                    |    } ]
                    |  } ]
                    |}""".stripMargin

  enum ExampleBehavior {
    case EAT, SLEEP, RAVE, RE_EAT
  }

  val treeFixture = SequenceNode(
    ActionNode(ExampleBehavior.EAT),
    ActionNode(ExampleBehavior.RE_EAT),
    SelectorNode(ActionNode(ExampleBehavior.SLEEP))
  )

  test("serializes to json") {
    val result = BTMapper.mapper.writeValueAsString(treeFixture)

    println(result)

    result shouldEqual jsonFixture
  }

  test("deserializes from json") {
    println(jsonFixture)
    val result =
      BTMapper.mapper.readValue(jsonFixture, classOf[Node[ExampleBehavior]])

    println(result)

    result shouldEqual treeFixture
  }
}
