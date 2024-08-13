package io.github.jugbot.ai

import io.github.jugbot.Mod.LOGGER

import scala.collection.mutable

type Parameters = Map[String, String]
type ParameterizedAction = (String, Parameters)
type ParameterizedNode = Node[ParameterizedAction]
type References = Map[String, ParameterizedNode]

case class BehaviorLog(name: String, args: Map[String, String], result: BehaviorStatus, isModule: Boolean) {
  override def toString: String = f"$name($args) => $result"
}

/**
 * Gets the state from a behavior tree, using a graph of subtrees which reference each other
 * @param root The entrypoint node to the tree/graph
 * @param perform Performs a behavior according to a string name and map of string parameters
 * @param references Map of subtrees and their names
 */
def runModules(root: ParameterizedNode,
               perform: Perform[ParameterizedAction],
               references: References = Map.empty
): Seq[BehaviorLog] = {
  val log = mutable.Buffer.empty[BehaviorLog]
  def cb(name: String, parameters: Parameters, context: Parameters): BehaviorStatus = {
    val hydratedParams = parameters.map { (key, value) =>
      value match {
        case s"$$${v}" => (key, context(v))
        case _         => (key, value)
      }
    }
    val index = log.size
    log.append(null)
    val status = references.get(name) match {
      case Some(n) => run(n, cb(_, _, hydratedParams))
      case None    => perform(name, hydratedParams)
    }
    log.update(index, BehaviorLog(name, hydratedParams, status, references.contains(name)))
    status
  }

  run(root, cb(_, _, Map.empty))
  log.toSeq
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

    def visitChildren(children: ParameterizedNode*) =
      children.foldLeft(visited)((acc, n) => acc ++ dfsNode(n, stack :+ node, visited + node))

    node match {
      case SelectorNode(children*) => visitChildren(children*)
      case SequenceNode(children*) => visitChildren(children*)
      case ActionNode(name, args) =>
        if m.contains(name) then {
          visitChildren(m(name))
        } else visited + node
      case ConditionNode(ifNode, thenNode, elseNode) => visitChildren(ifNode, thenNode, elseNode)
    }
  }

  val allNodes = dfsNode(m("root"))
  allNodes.forall { node =>
    node match {
      case ActionNode(name, args) =>
        // TODO: Verify var args are satisfied.
        if serializerTest(name, args).isDefined || m.contains(name) then true
        else
          LOGGER.error(s"Could not map '$name' to any known behavior with arguments $args")
          false
      case _ => true
    }
  }
}
