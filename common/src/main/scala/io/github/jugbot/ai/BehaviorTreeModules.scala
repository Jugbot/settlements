package io.github.jugbot.ai

/**
  * Extension of BehaviorTree that adds support for modularized BTs and module-scoped variables 
  */

type Parameters = Map[String, String]
type ParameterizedAction = (String, Parameters)
type ParameterizedNode = Node[ParameterizedAction]
type References = Map[String, ParameterizedNode]

// Method for
//def getState(node: ParameterizedNode,
//             perform: Perform[ParameterizedAction],
//             references: References = Map.empty,
//             parameters: Parameters = Map.empty
//): Status = node match {
//  case ActionNode(name, args) =>
//    val hydratedArgs = args.map((key, value) => (key, parameters.getOrElse(value, value)))
//    references.get(name) match {
//      case Some(n) => getState(n, perform, references, hydratedArgs)
//      case None    => perform(name, hydratedArgs)
//    }
//  case SequenceNode(children*) => runSequence(children, getState(_, perform, references, parameters))
//  case SelectorNode(children*) => runSelector(children, getState(_, perform, references, parameters))
//}

def getState(node: ParameterizedNode,
             perform: Perform[ParameterizedAction],
             references: References = Map.empty
): Status = {
  def cb(name: String, parameters: Parameters, context: Parameters): Status = {
    val hydratedParams = parameters.map((key, value) => (key, context.getOrElse(value, value)))
    references.get(name) match {
      case Some(n) => run(n, cb(_, _, hydratedParams))
      case None    => perform(name, hydratedParams)
    }
  }

  run(node, cb(_, _, Map.empty))
}
