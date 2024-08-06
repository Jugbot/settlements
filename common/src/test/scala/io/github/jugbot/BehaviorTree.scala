package io.github.jugbot

import io.github.jugbot.ai.{
  run as state,
  ActionNode,
  BehaviorFailure,
  BehaviorStatus,
  BehaviorSuccess,
  IfElseNode,
  Node,
  SelectorNode,
  SequenceNode
}
import net.minecraft.world.entity.ai.behavior.Behavior
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class BehaviorTree extends UnitSuite {
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
            BehaviorSuccess
          case ExampleBehaviors.RE_EAT =>
            BehaviorSuccess
          case _ =>
            BehaviorFailure
        }
    )

    assert(result == BehaviorSuccess)
  }

  val actionSuccess: Function[Int, BehaviorStatus] = (a: Int) => BehaviorSuccess
  val actionFailure: Function[Int, BehaviorStatus] = (a: Int) => BehaviorFailure

  test("ActionNode returns success when action success") {
    val node = ActionNode(5)
    state(node, actionSuccess) should equal(BehaviorSuccess)
  }

  test("ActionNode returns failure when action failed") {
    val node = ActionNode(5)
    state(node, actionFailure) should equal(BehaviorFailure)
  }

  test("SequenceNode returns success if all actions are success") {
    val node = SequenceNode(ActionNode(1), ActionNode(2), ActionNode(3))
    state(node, actionSuccess) should equal(BehaviorSuccess)
  }

  test("SequenceNode returns failure if any action is failure") {
    val node = SequenceNode(ActionNode(1), ActionNode(2), ActionNode(3))
    state(node, actionFailure) should equal(BehaviorFailure)
  }

  test("SelectorNode returns success if any action is success") {
    val node = SelectorNode(ActionNode(1), ActionNode(2), ActionNode(3))
    state(node, actionSuccess) should equal(BehaviorSuccess)
  }

  test("SelectorNode returns failure all actions are failure") {
    val node = SelectorNode(ActionNode(1), ActionNode(2), ActionNode(3))
    state(node, actionFailure) should equal(BehaviorFailure)
  }

  test("ConditionNode returns failure 1") {
    val node = IfElseNode(ActionNode(BehaviorFailure), ActionNode(BehaviorSuccess), ActionNode(BehaviorFailure))
    state(node, identity[BehaviorStatus]) should equal(BehaviorFailure)
  }

  test("ConditionNode returns failure 2") {
    val node = IfElseNode(ActionNode(BehaviorSuccess), ActionNode(BehaviorFailure), ActionNode(BehaviorSuccess))
    state(node, identity[BehaviorStatus]) should equal(BehaviorFailure)
  }

  test("ConditionNode returns success 1") {
    val node = IfElseNode(ActionNode(BehaviorSuccess), ActionNode(BehaviorSuccess), ActionNode(BehaviorFailure))
    state(node, identity[BehaviorStatus]) should equal(BehaviorSuccess)
  }

  test("ConditionNode returns success 2") {
    val node = IfElseNode(ActionNode(BehaviorFailure), ActionNode(BehaviorFailure), ActionNode(BehaviorSuccess))
    state(node, identity[BehaviorStatus]) should equal(BehaviorSuccess)
  }
}
