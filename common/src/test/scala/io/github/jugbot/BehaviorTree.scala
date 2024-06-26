package io.github.jugbot

import io.github.jugbot.ai.ActionNode
import io.github.jugbot.ai.Failure
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.SelectorNode
import io.github.jugbot.ai.SequenceNode
import io.github.jugbot.ai.Status
import io.github.jugbot.ai.Success
import org.scalatest.funsuite.AnyFunSuite
import io.github.jugbot.ai.state
import org.scalatest.matchers.should.Matchers

class BehaviorTree extends AnyFunSuite with Matchers {
  test("example behavior tree") {
    enum ExampleBehaviors {
      case EAT
      case SLEEP
      case RAVE
      case RE_EAT
    }

    val bt = SequenceNode(
      ActionNode(ExampleBehaviors.EAT),
      ActionNode(ExampleBehaviors.RE_EAT)
    )

    val result = state(
      bt,
      (behavior: ExampleBehaviors) =>
        behavior match {
          case ExampleBehaviors.EAT =>
            print("EAT")
            Success
          case ExampleBehaviors.RE_EAT =>
            print("RE_EAT")
            Success
          case _ =>
            print("OTHER")
            Failure
        }
    )

    assert(result == Success)
  }

  val actionSuccess: Function[Int, Status] = (a: Int) => Success
  val actionFailure: Function[Int, Status] = (a: Int) => Failure

  test("ActionNode returns success when action success") {
    val node = ActionNode(5)
    state(node, actionSuccess) should equal(Success)
  }

  test("ActionNode returns failure when action failed") {
    val node = ActionNode(5)
    state(node, actionFailure) should equal(Failure)
  }

  test("SequenceNode returns success if all actions are success") {
    val node = SequenceNode(ActionNode(1), ActionNode(2), ActionNode(3))
    state(node, actionSuccess) should equal(Success)
  }

  test("SequenceNode returns failure if any action is failure") {
    val node = SequenceNode(ActionNode(1), ActionNode(2), ActionNode(3))
    state(node, actionFailure) should equal(Failure)
  }

  test("SelectorNode returns success if any action is success") {
    val node = SelectorNode(ActionNode(1), ActionNode(2), ActionNode(3))
    state(node, actionSuccess) should equal(Success)
  }

  test("SelectorNode returns failure all actions are failure") {
    val node = SelectorNode(ActionNode(1), ActionNode(2), ActionNode(3))
    state(node, actionFailure) should equal(Failure)
  }
}
