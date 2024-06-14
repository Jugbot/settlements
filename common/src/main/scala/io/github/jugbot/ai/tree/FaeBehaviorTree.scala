package io.github.jugbot.ai.tree

import io.github.jugbot.ai.SequenceNode
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.ActionNode
import io.github.jugbot.ai.SelectorNode

enum FaeBehavior {
  case unknown
  case unimplemented
  case sleep
  case is_tired
  case has_valid_bed
  case claim_bed
  case is_at_location
  case has_nav_path
  case get_nav_path
  case path_unobstructed
  case move_along_path
}

object FaeBehaviorTree {
  private val goToBlock = SelectorNode(
    ActionNode(FaeBehavior.is_at_location),
    SequenceNode(
      SelectorNode(ActionNode(FaeBehavior.has_nav_path), ActionNode(FaeBehavior.get_nav_path)),
      ActionNode(FaeBehavior.path_unobstructed),
      ActionNode(FaeBehavior.move_along_path)
    )
  )
  private val claimBed = ActionNode(FaeBehavior.claim_bed)
  private val sleep = SequenceNode(
    ActionNode(FaeBehavior.is_tired),
    SelectorNode(
      ActionNode(FaeBehavior.has_valid_bed),
      claimBed
    ),
    goToBlock,
    ActionNode(FaeBehavior.sleep)
  )
  private val survival = SequenceNode(sleep)
  val root: Node[FaeBehavior] = SequenceNode(survival)
}
