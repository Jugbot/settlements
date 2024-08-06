package io.github.jugbot.ai

import io.github.jugbot.Mod.LOGGER

type Parameters = Map[String, String]
type ParameterizedAction = (String, Parameters)
type ParameterizedNode = Node[ParameterizedAction]
type References = Map[String, ParameterizedNode]

/**
 * Gets the state from a behavior tree, using a graph of subtrees which reference each other
 * @param root The entrypoint node to the tree/graph
 * @param perform Performs a behavior according to a string name and map of string parameters
 * @param references Map of subtrees and their names
 */
def runModules(root: ParameterizedNode,
               perform: Perform[ParameterizedAction],
               references: References = Map.empty
): Unit = {
  def cb(name: String, parameters: Parameters, context: Parameters): BehaviorStatus = {
    val hydratedParams = parameters.map { (key, value) =>
      value match {
        case s"$$${v}" => (key, context(v))
        case _         => (key, value)
      }
    }
    references.get(name) match {
      case Some(n) => run(n, cb(_, _, hydratedParams))
      case None    => perform(name, hydratedParams)
    }
  }

  run(root, cb(_, _, Map.empty))
}

type SerializerTest = (name: String, args: Map[String, String]) => Option[Any]

// TODO: Should return error message instead of boolean
def isValidTree(m: Map[String, ParameterizedNode], serializerTest: SerializerTest): Boolean = {
  if !m.contains("root") then return false

  def dfsNode(node: ParameterizedNode,
              stack: Seq[ParameterizedNode] = Seq.empty,
              visited: Set[ParameterizedNode] = Set.empty
  ): Set[ParameterizedNode] = {
    if stack.contains(node) then
      LOGGER.warn(
        s"Found a cyclic chain of behaviors, this could lead to performance problems!\n\tCycle: ${stack.mkString(" -> ")} -> $node"
      )
    if visited.contains(node) then return visited

    def visitChildren(children: Seq[ParameterizedNode]) =
      children.foldLeft(visited)((acc, n) => dfsNode(n, stack :+ node, visited + node))

    node match {
      case SelectorNode(children*) => visitChildren(children)
      case SequenceNode(children*) => visitChildren(children)
      case ActionNode(name, args) =>
        if m.contains(name) then {
          dfsNode(m(name), stack :+ node, visited + node)
        } else visited + node
      case ConditionNode(ifNode, thenNode, elseNode) => visitChildren(Seq(ifNode, thenNode, elseNode))
    }
  }
  def isValidNode(node: ParameterizedNode): Boolean =
    dfsNode(node).forall { node =>
      node match {
        case ActionNode(name, args) =>
          // TODO: Verify var args are satisfied.
          if serializerTest(name, args).isDefined || (m.contains(name) && isValidNode(m(name))) then true
          else
            LOGGER.error(s"Could not map '$name' to any known behavior with arguments $args")
            false
        case _ => true
      }
    }

  isValidNode(m("root"))
}
