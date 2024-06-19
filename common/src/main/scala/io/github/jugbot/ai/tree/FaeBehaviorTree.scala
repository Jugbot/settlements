package io.github.jugbot.ai.tree

import io.github.jugbot.ai.SequenceNode
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.ActionNode
import io.github.jugbot.ai.SelectorNode

enum BlackboardKey {
  case bed_position
}

enum FaeBehavior(args: BlackboardKey*) {
  case unknown
  case unimplemented
  case sleep
  case is_tired
  case has(key: BlackboardKey) extends FaeBehavior(key)
  case claim_bed
  case bed_is_valid
  case is_at_location(destinationKey: BlackboardKey) extends FaeBehavior(destinationKey)
  case has_nav_path_to(destinationKey: BlackboardKey) extends FaeBehavior(destinationKey)
  case create_nav_path_to(destinationKey: BlackboardKey) extends FaeBehavior(destinationKey)
  case current_path_unobstructed
  case move_along_current_path
}

object FaeBehavior {
  def valueOf(jsonValue: String): FaeBehavior = {
    val tokens = jsonValue.split("""[(),]""").toList

    // TODO: This could probably be done automatically using reflection or macros
    tokens match {
      case behaviorType :: args =>
        val keys = args.map(BlackboardKey.valueOf)
        behaviorType match {
          case "unimplemented"           => FaeBehavior.unimplemented
          case "has" if keys.length >= 1 => FaeBehavior.has(keys(0))
          case _                         => FaeBehavior.unknown
        }
      case _ => FaeBehavior.unknown
    }
  }
}

object FaeBehaviorTree {
  private def goToBlock(key: BlackboardKey): Node[FaeBehavior] = SelectorNode(
    ActionNode(FaeBehavior.is_at_location(key)),
    SequenceNode(
      SelectorNode(ActionNode(FaeBehavior.has_nav_path_to(key)), ActionNode(FaeBehavior.create_nav_path_to(key))),
      ActionNode(FaeBehavior.current_path_unobstructed),
      ActionNode(FaeBehavior.move_along_current_path)
    )
  )
  private val claimBed = ActionNode(FaeBehavior.claim_bed)
  private val sleep = SequenceNode(
    ActionNode(FaeBehavior.is_tired),
    SelectorNode(
      SequenceNode(
        ActionNode(FaeBehavior.has(BlackboardKey.bed_position)),
        ActionNode(FaeBehavior.bed_is_valid)
      ),
      claimBed
    ),
    goToBlock(BlackboardKey.bed_position),
    ActionNode(FaeBehavior.sleep)
  )
  private val survival = SequenceNode(sleep)
  val root: Node[FaeBehavior] = SequenceNode(survival)
}
