package io.github.jugbot.ai

type Perform[A] = Function[A, BehaviorStatus]

sealed trait Node[A]
case class ActionNode[A](action: A) extends Node[A]
case class SequenceNode[A](children: Node[A]*) extends Node[A]
case class SelectorNode[A](children: Node[A]*) extends Node[A]
case class ConditionNode[A](ifNode: Node[A], thenNode: Node[A], elseNode: Node[A]) extends Node[A]

// Status Enum
sealed trait BehaviorStatus
case object BehaviorSuccess extends BehaviorStatus
case object BehaviorFailure extends BehaviorStatus
case object BehaviorRunning extends BehaviorStatus

// Method for running nodes recursively
def run[A](node: Node[A], perform: Perform[A]): BehaviorStatus = node match {
  case ActionNode(action)      => perform(action)
  case SequenceNode(children*) => runSequence(children, run(_, perform))
  case SelectorNode(children*) => runSelector(children, run(_, perform))
  case value: ConditionNode[A] => runIfElse(value, run(_, perform))
}

private def runIfElse[A](node: ConditionNode[A], cb: Function[Node[A], BehaviorStatus]) = {
  val result = cb(node.ifNode)
  result match {
    case BehaviorSuccess => cb(node.thenNode)
    case BehaviorFailure => cb(node.elseNode)
    case _               => result
  }
}

// Method for running sequence nodes
private def runSequence[A](
  nodes: Seq[Node[A]],
  cb: Function[Node[A], BehaviorStatus]
): BehaviorStatus =
  nodes.foldLeft[BehaviorStatus](BehaviorSuccess) { (acc, node) =>
    if acc == BehaviorSuccess then cb(node)
    else acc
  }

// Method for running selector nodes
private def runSelector[A](
  nodes: Seq[Node[A]],
  cb: Function[Node[A], BehaviorStatus]
): BehaviorStatus =
  nodes.foldLeft[BehaviorStatus](BehaviorFailure) { (acc, node) =>
    if acc == BehaviorFailure then cb(node)
    else acc
  }
