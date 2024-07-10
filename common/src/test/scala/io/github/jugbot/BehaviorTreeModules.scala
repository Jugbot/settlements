package io.github.jugbot

import io.github.jugbot.ai.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ArrayBuffer

object ExampleBehaviors {
  val EAT = "EAT"
  val SLEEP = "SLEEP"
  val RAVE = "RAVE"
  val REPEAT = "REPEAT"
}

val eating: ParameterizedNode = SequenceNode(
  ActionNode(ExampleBehaviors.EAT,
             Map(
               "food" -> "$food"
             )
  )
)

def perform(result: ArrayBuffer[String])(action: String, parameters: Map[String, String]): Status = {
  result += action
  action match {
    case ExampleBehaviors.EAT if parameters.contains("food") =>
      result += parameters("food")
      Success
    case ExampleBehaviors.REPEAT =>
      Success
    case ExampleBehaviors.SLEEP =>
      Success
    case ExampleBehaviors.RAVE =>
      Success
    case unknown =>
      throw Exception(f"unknown action $unknown")
  }
}

class BehaviorTreeModules extends AnyFunSuite with Matchers {
  test("calls module with parameters") {
    val result = ArrayBuffer.empty[String]
    val root: ParameterizedNode = SequenceNode(
      ActionNode("eating",
                 Map(
                   "$food" -> "bagel"
                 )
      )
    )

    runModules(
      root,
      perform(result),
      Map(
        "eating" -> eating
      )
    )

    (result should contain).theSameElementsInOrderAs(
      Seq(ExampleBehaviors.EAT, "bagel")
    )
  }

  test("calls modules with different parameters") {
    val result = ArrayBuffer.empty[String]
    val root: ParameterizedNode = SequenceNode(
      ActionNode("eating",
                 Map(
                   "$food" -> "bagel"
                 )
      ),
      ActionNode("eating",
                 Map(
                   "$food" -> "pizza"
                 )
      )
    )

    runModules(
      root,
      perform(result),
      Map(
        "eating" -> eating
      )
    )

    (result should contain).theSameElementsInOrderAs(
      Seq(ExampleBehaviors.EAT, "bagel", ExampleBehaviors.EAT, "pizza")
    )
  }
}
