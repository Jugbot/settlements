package io.github.jugbot

import io.github.jugbot.ai.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

private enum ExampleBehavior {
  case UNKNOWN
  case EAT
  case SLEEP
  case RAVE
  case REPEAT
}

object ExampleBehavior {
  def valueOf(s: String, default: ExampleBehavior): ExampleBehavior = Try(ExampleBehavior.valueOf(s)) match {
    case scala.util.Failure(exception) => default
    case scala.util.Success(value)     => value
  }
}

val eating: ParameterizedNode = SequenceNode(
  ActionNode(ExampleBehavior.EAT.toString,
             Map(
               "food" -> "$food"
             )
  )
)

def perform(result: ArrayBuffer[String])(action: String, parameters: Map[String, String]): BehaviorStatus = {
  result += action
  ExampleBehavior.valueOf(action, ExampleBehavior.UNKNOWN) match {
    case ExampleBehavior.EAT if parameters.contains("food") =>
      result += parameters("food")
      BehaviorSuccess
    case ExampleBehavior.REPEAT =>
      BehaviorSuccess
    case ExampleBehavior.SLEEP =>
      BehaviorSuccess
    case ExampleBehavior.RAVE =>
      BehaviorSuccess
    case _ =>
      throw Exception(f"unknown action $action")
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
      Seq(ExampleBehavior.EAT.toString, "bagel")
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
      Seq(ExampleBehavior.EAT.toString, "bagel", ExampleBehavior.EAT.toString, "pizza")
    )
  }
}
