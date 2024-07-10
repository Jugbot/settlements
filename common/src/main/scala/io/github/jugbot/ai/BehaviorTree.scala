package io.github.jugbot.ai

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
def state[A](node: Node[A], action: Function[A, Status]): Status = node match {
  case ActionNode(a)           => action(a)
  case SequenceNode(children*) => runSequence(children, n => state(n, action))
  case SelectorNode(children*) => runSelector(children, n => state(n, action))
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

type Parameters = Map[String, String]
type References[A] = Map[String, ParameterizedNode[A]]

sealed trait ParameterizedNode[A] extends Node[(A, Parameters)]
case class ReferenceNode[A](name: String, params: Parameters) extends ParameterizedNode[A]

def run[A](node: ParameterizedNode[A],
                  perform: Performance[A],
                  references: References[A] = Map.empty,
                  parameters: Parameters = Map.empty
  ): Status = node match {
    case ReferenceNode(name, args) => {
      val hydratedArgs = args.map((key, value) => (key, parameters.getOrElse(value, value)))
      references.get(name) match {
        case Some(n) => run(n, perform, references, hydratedArgs)
        case None => perform(name, hydratedArgs)
      }
    }
    case ActionNode(name, args) => 
      val hydratedArgs = args.map((key, value) => (key, parameters.getOrElse(value, value)))
      perform(name, hydratedArgs)
  }