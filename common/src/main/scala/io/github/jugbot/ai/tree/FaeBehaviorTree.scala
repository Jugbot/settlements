package io.github.jugbot.ai.tree

import io.github.jugbot.ai.SequenceNode
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.ActionNode
import io.github.jugbot.ai.SelectorNode
import scala.reflect.ClassTag
import scala.deriving.Mirror

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
          case "unimplemented" => FaeBehavior.unimplemented
          case "has" if args.length >= 1 => FaeBehavior.has(args(0))
          case _        => FaeBehavior.unknown
        }
      case _ => FaeBehavior.unknown
    }
  }
}

object FaeBehaviorTree {
  private val goToBlock: Node[FaeBehavior] = SelectorNode(
    ActionNode(FaeBehavior.is_at_location("bed_position")),
    SequenceNode(
      SelectorNode(ActionNode(FaeBehavior.has_nav_path_to("bed_position")), ActionNode(FaeBehavior.create_nav_path_to("bed_position"))),
      ActionNode(FaeBehavior.current_path_unobstructed),
      ActionNode(FaeBehavior.move_along_current_path)
    )
  )
  private val claimBed = ActionNode(FaeBehavior.claim_bed)
  private val sleep = SequenceNode(
    ActionNode(FaeBehavior.is_tired),
    SelectorNode(
      ActionNode(FaeBehavior.has("bed_position")),
      claimBed
    ),
    goToBlock,
    ActionNode(FaeBehavior.sleep)
  )
  private val survival = SequenceNode(sleep)
  val root: Node[FaeBehavior] = SequenceNode(survival)
}
