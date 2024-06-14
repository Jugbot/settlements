package io.github.jugbot.ai.tree

import io.github.jugbot.ai.SequenceNode
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.ActionNode
import io.github.jugbot.ai.SelectorNode

enum FaeBehavior {
  case unknown extends FaeBehavior
  case unimplemented extends FaeBehavior
  case sleep extends FaeBehavior
  case is_tired extends FaeBehavior
  case has_valid_bed extends FaeBehavior
  case claim_bed extends FaeBehavior
}

object FaeBehaviorTree {
  private val goToBlock = ActionNode(FaeBehavior.unimplemented)
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
