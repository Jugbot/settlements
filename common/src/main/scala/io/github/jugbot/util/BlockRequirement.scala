package io.github.jugbot.util

import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

case class BlockStateRequirement(state: TagKey[Block] | Block | BlockState, total: Int, satisfied: Int = 0) {
  private def getPredicate: BlockState => Boolean =
    state match {
      case block: Block =>
        (bs: BlockState) => bs.getBlock == block
      case blockState: BlockState =>
        (bs: BlockState) => bs.is(blockState.getBlock) && bs.getProperties.containsAll(blockState.getProperties)
      case tagKey: TagKey[Block] =>
        (bs: BlockState) => bs.is(tagKey)
    }

  def tally(blockState: BlockState): BlockStateRequirement =
    if isFullySatisfied then this
    else if this.getPredicate(blockState) then BlockStateRequirement(state, total, satisfied + 1)
    else this

  def isFullySatisfied: Boolean = total == satisfied

  def toRenderableString: String =
    s"${state.toString} ($satisfied/$total)"
}
