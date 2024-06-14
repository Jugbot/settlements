package io.github.jugbot

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat.Feature
import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.EnumModule
import io.github.jugbot.ai.ActionNode
import io.github.jugbot.ai.BTMapper
import io.github.jugbot.ai.BTModule
import io.github.jugbot.ai.Failure
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.SelectorNode
import io.github.jugbot.ai.SequenceNode
import io.github.jugbot.ai.Status
import io.github.jugbot.ai.Success
import io.github.jugbot.ai.state
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.annotation.targetName

enum ExampleBehavior {
  case eat extends ExampleBehavior
  case sleep extends ExampleBehavior
  case rave extends ExampleBehavior
  case re_eat extends ExampleBehavior
}

class BTSerialization extends AnyFunSuite with Matchers {
  val jsonFixture: String = """{
                              |  "sequence" : [ {
                              |    "action" : "eat"
                              |  }, {
                              |    "action" : "re_eat"
                              |  }, {
                              |    "selector" : [ {
                              |      "action" : "sleep"
                              |    } ]
                              |  } ]
                              |}""".stripMargin

  val treeFixture: SequenceNode[ExampleBehavior] = SequenceNode(
    ActionNode(ExampleBehavior.eat),
    ActionNode(ExampleBehavior.re_eat),
    SelectorNode(ActionNode(ExampleBehavior.sleep))
  )

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
