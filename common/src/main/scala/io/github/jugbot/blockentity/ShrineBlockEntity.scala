package io.github.jugbot.blockentity

import com.google.common.base.Suppliers
import io.github.jugbot.block.ShrineBlock
import net.minecraft.core.BlockPos
import net.minecraft.nbt.{CompoundTag, ListTag, NbtUtils, Tag}
import net.minecraft.tags.{BlockTags, TagKey}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.{Block, Blocks}
import net.minecraft.world.phys.AABB

import java.util.UUID
import java.util.function.Supplier
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.StreamConverters.*

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

// TODO: Convert to datapack config
val shrineTierRequirements = List(
  Set(
    BlockStateRequirement(BlockTags.LOGS, 16),
    BlockStateRequirement(BlockTags.LEAVES, 32)
  ),
  Set(
    BlockStateRequirement(BlockTags.STONE_BRICKS, 16)
  ),
  Set(
    BlockStateRequirement(Blocks.LANTERN, 8)
  ),
  Set(
    BlockStateRequirement(Blocks.GOLD_BLOCK, 4)
  ),
  Set(
    BlockStateRequirement(BlockTags.CANDLES, 5)
  ),
  Set(
    BlockStateRequirement(BlockTags.TERRACOTTA, 16)
  )
)

class UUIDListTag extends ListTag {
  def this(list: List[UUID]) = {
    this()
    list.map(NbtUtils.createUUID).foreach(tag => this.addTag(0, tag))
  }

}

class ShrineBlockEntity(pos: BlockPos, state: BlockState)
    extends BlockEntity(ShrineBlockEntity.TYPE.get(), pos, state) {
  var tier: Int = 0
  var nextTierRequirements: Set[BlockStateRequirement] = Set.empty
  var owners = mutable.Set.empty[UUID]

  private val TAG_TIER = "tier"
  private val TAG_OWNERS = "owners"

  override def load(compoundTag: CompoundTag): Unit =
    super.load(compoundTag)
    tier = compoundTag.getInt(TAG_TIER)
    owners =
      compoundTag.getList(TAG_OWNERS, Tag.TAG_INT_ARRAY).iterator().asScala.map(NbtUtils.loadUUID).to(mutable.Set)

  override def saveAdditional(compoundTag: CompoundTag): Unit = {
    super.saveAdditional(compoundTag)
    compoundTag.putInt(TAG_TIER, tier)
    compoundTag.put(TAG_OWNERS, UUIDListTag(owners.toList))
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
