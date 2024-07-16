package io.github.jugbot.ai

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
