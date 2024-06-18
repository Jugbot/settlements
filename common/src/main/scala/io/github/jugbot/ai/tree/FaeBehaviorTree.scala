package io.github.jugbot.ai.tree

import io.github.jugbot.ai.SequenceNode
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.ActionNode
import io.github.jugbot.ai.SelectorNode

enum FaeBehavior(args: String*) {
  case unknown
  case unimplemented
  case sleep
  case is_tired
  case has(key: String) extends FaeBehavior(key)
  case claim_bed
  case is_at_location(destinationKey: String) extends FaeBehavior(destinationKey)
  case has_nav_path_to(destinationKey: String) extends FaeBehavior(destinationKey)
  case create_nav_path_to(destinationKey: String) extends FaeBehavior(destinationKey)
  case current_path_unobstructed
  case move_along_current_path
}

object FaeBehavior {
  def valueOf(jsonValue: String): FaeBehavior = {
    val tokens = jsonValue.split("""[\(\),]""").toList

    // TODO: This could probably be done automatically using reflection or macros
    tokens match {
      case behaviorType :: args =>
        behaviorType match {
          case "unimplemented"           => FaeBehavior.unimplemented
          case "has" if args.length >= 1 => FaeBehavior.has(args(0))
          case _                         => FaeBehavior.unknown
        }
      case _ => FaeBehavior.unknown
    }
  }
}

object BlackboardKey {
  val BED_POSITION = "bed_position"
}

object FaeBehaviorTree {
  private def goToBlock(key: String): Node[FaeBehavior] = SelectorNode(
    ActionNode(FaeBehavior.is_at_location(key)),
    SequenceNode(
      SelectorNode(ActionNode(FaeBehavior.has_nav_path_to(key)),
                   ActionNode(FaeBehavior.create_nav_path_to(key))
      ),
      ActionNode(FaeBehavior.current_path_unobstructed),
      ActionNode(FaeBehavior.move_along_current_path)
    )
  )
  private val claimBed = ActionNode(FaeBehavior.claim_bed)
  private val sleep = SequenceNode(
    ActionNode(FaeBehavior.is_tired),
    SelectorNode(
      ActionNode(FaeBehavior.has(BlackboardKey.BED_POSITION)),
      claimBed
    ),
    goToBlock(BlackboardKey.BED_POSITION),
    ActionNode(FaeBehavior.sleep)
  )
  private val survival = SequenceNode(sleep)
  val root: Node[FaeBehavior] = SequenceNode(survival)
}
