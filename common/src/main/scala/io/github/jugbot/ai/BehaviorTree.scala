package io.github.jugbot.ai

sealed trait Node
case class ActionNode(name: String, args: Map[String, String] = Map.empty) extends Node
case class SequenceNode(children: Node*) extends Node
case class SelectorNode(children: Node*) extends Node

// Status Enum
sealed trait Status
case object Success extends Status
case object Failure extends Status
case object Running extends Status

private object BehaviorTree {
  // Used for evaluating args that reference some variable stored in ctx instead of a constant
  private def hydrator(args: Map[String, String], ctx: Map[String, String]): Map[String, String] =
      args.map((key, value) => (key, ctx.getOrElse(value, value)))

  // Method for running nodes
  private def run(node: Node,
                  perform: (name: String, args: Map[String, String]) => Status,
                  references: Map[String, Node] = Map.empty,
                  context: Map[String, String] = Map.empty
  ): Status = node match {
    case SequenceNode(children*) => runSequence(children, n => run(n, perform, references, context))
    case SelectorNode(children*) => runSelector(children, n => run(n, perform, references, context))
    case ActionNode(name, args) =>
      val hydratedArgs = hydrator(args, context)
      references.get(name) match {
        case Some(n) => run(n, perform, references, hydratedArgs)
        // TODO: merge hardcoded values to context, or just make a new parameter Seq[String] for the args specific to the action
        case None => perform(name, hydratedArgs)
      }
  }

  // Method for running sequence nodes
  private def runSequence(
    nodes: Seq[Node],
    cb: Function[Node, Status]
  ): Status =
    nodes.foldLeft[Status](Success) { (acc, node) =>
      if acc == Success then cb(node)
      else acc
    }

  // Method for running selector nodes
  private def runSelector(
    nodes: Seq[Node],
    cb: Function[Node, Status]
  ): Status =
    nodes.foldLeft[Status](Failure) { (acc, node) =>
      if acc == Failure then cb(node)
      else acc
    }
}

class BehaviorTree(val trees: Map[String, Node]) {
  def state(perform: (String, Map[String, String]) => Status): Status =
    trees.get("root") match {
      case Some(root) =>
        BehaviorTree.run(root, perform, trees)
      case None =>
        Failure
    }
}
