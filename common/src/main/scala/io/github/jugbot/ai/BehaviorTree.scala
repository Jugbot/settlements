package io.github.jugbot.ai

type Perform[A] = Function[A, Status]

sealed trait Node[A]
case class ActionNode[A](action: A) extends Node[A]
case class SequenceNode[A](children: Node[A]*) extends Node[A]
case class SelectorNode[A](children: Node[A]*) extends Node[A]

// Status Enum
sealed trait Status
case object Success extends Status
case object Failure extends Status
case object Running extends Status

// Method for running nodes
def run[A](node: Node[A], perform: Perform[A]): Status = node match {
  case ActionNode(action)      => perform(action)
  case SequenceNode(children*) => runSequence(children, run(_, perform))
  case SelectorNode(children*) => runSelector(children, run(_, perform))
}

// Method for running sequence nodes
private def runSequence[A](
  nodes: Seq[Node[A]],
  cb: Function[Node[A], Status]
): Status =
  nodes.foldLeft[Status](Success) { (acc, node) =>
    if acc == Success then cb(node)
    else acc
  }

// Method for running selector nodes
private def runSelector[A](
  nodes: Seq[Node[A]],
  cb: Function[Node[A], Status]
): Status =
  nodes.foldLeft[Status](Failure) { (acc, node) =>
    if acc == Failure then cb(node)
    else acc
  }
