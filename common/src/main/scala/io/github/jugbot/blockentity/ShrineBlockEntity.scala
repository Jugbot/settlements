package io.github.jugbot.blockentity

import com.google.common.base.Suppliers
import io.github.jugbot.block.ShrineBlock
import net.minecraft.BlockUtil
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.tags.{BlockTags, TagKey}
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.{AbstractContainerMenu, CraftingMenu}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.{Block, Blocks}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import scala.jdk.StreamConverters.*

import java.util.function.Supplier

case class BlockStateRequirement(val state: TagKey[Block] | Block | BlockState,
                                 val total: Int,
                                 val satisfied: Int = 0
) {
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

  def toRenderableString: String = {
    s"${state.toString} ($satisfied/$total)"
  }
}

// TODO: Convert to datapack config
val shrineTierRequirements = List(
  Set(
    BlockStateRequirement(BlockTags.CANDLES, 1)
  ),
  Set(
    BlockStateRequirement(Blocks.CRAFTING_TABLE, 1)
  )
)

class ShrineBlockEntity(pos: BlockPos, state: BlockState)
    extends BlockEntity(ShrineBlockEntity.TYPE.get(), pos, state) {
  var tier: Int = 0
  var nextTierRequirements: Set[BlockStateRequirement] = Set.empty

  private val TAG_TIER = "tier"

  override def load(compoundTag: CompoundTag): Unit =
    super.load(compoundTag)
    tier = compoundTag.getInt(TAG_TIER)

  override def saveAdditional(compoundTag: CompoundTag): Unit = {
    super.saveAdditional(compoundTag)
    compoundTag.putInt(TAG_TIER, tier)
  }
}

object ShrineBlockEntity {
  val blockRadius = 8

  final val TYPE: Supplier[BlockEntityType[ShrineBlockEntity]] = Suppliers.memoize(() =>
    BlockEntityType.Builder
      .of[ShrineBlockEntity](new ShrineBlockEntity(_, _), ShrineBlock.INSTANCE)
      // TODO: what should this be if not null?
      .build(null)
  )

  private def searchArea(pos: BlockPos) = AABB(pos.offset(-blockRadius, -blockRadius, -blockRadius),
                                               pos.offset(blockRadius, blockRadius, blockRadius).offset(1, 1, 1)
  )

  private def getUnsatisfiedRequirements[T](level: Level,
                                            pos: BlockPos,
                                            requirements: Set[BlockStateRequirement]
  ): Set[BlockStateRequirement] = {
    val blocks = level
      .getBlockStates(
        searchArea(pos)
      )
      .toScala(LazyList)
      .toList
    requirements.map(req => blocks.foldLeft(req)((acc, bs) => acc.tally(bs)))
  }

  // TODO: Make the requirement check async since more than one shrine causes large performance dips
  def tick(level: Level, pos: BlockPos, blockState: BlockState, unknownBlockEntity: BlockEntity): Unit = {
    if !unknownBlockEntity.isInstanceOf[ShrineBlockEntity] then return
    val shrineBlockEntity = unknownBlockEntity.asInstanceOf[ShrineBlockEntity]
    val unsatisfiedRequirements = shrineTierRequirements.map(reqs => getUnsatisfiedRequirements(level, pos, reqs))
    val maybeNextTierIndex =
      unsatisfiedRequirements.indexWhere(requirements => requirements.exists(req => !req.isFullySatisfied))
    val nextTier = if maybeNextTierIndex == -1 then unsatisfiedRequirements.length else maybeNextTierIndex
    val nextRequirements =
      if maybeNextTierIndex == -1 then Set.empty[BlockStateRequirement] else unsatisfiedRequirements(maybeNextTierIndex)

    shrineBlockEntity.tier = nextTier
    shrineBlockEntity.nextTierRequirements = nextRequirements
  }

}
