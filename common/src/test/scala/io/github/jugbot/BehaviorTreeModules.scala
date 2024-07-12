package io.github.jugbot

import io.github.jugbot.ai.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ArrayBuffer

private object ExampleBehavior {
  val EAT = "eat"
  val SLEEP = "sleep"
  val RAVE = "rave"
  val REPEAT = "repeat"
}

val eating: ParameterizedNode = SequenceNode(
  ActionNode(ExampleBehavior.EAT,
             Map(
               "food" -> "$food"
             )
  )
)

def perform(result: ArrayBuffer[String])(action: String, parameters: Map[String, String]): Status = {
  result += action
  action match {
    case ExampleBehavior.EAT if parameters.contains("food") =>
      result += parameters("food")
      Success
    case ExampleBehavior.REPEAT =>
      Success
    case ExampleBehavior.SLEEP =>
      Success
    case ExampleBehavior.RAVE =>
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
      Seq(ExampleBehavior.EAT, "bagel")
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
      Seq(ExampleBehavior.EAT, "bagel", ExampleBehavior.EAT, "pizza")
    )
  }
}
