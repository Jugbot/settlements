package io.github.jugbot.blockentity

import com.google.common.base.Suppliers
import io.github.jugbot.block.ShrineBlock
import io.github.jugbot.entity.zone.ShrineZoneEntity
import io.github.jugbot.extension.CompoundTag.{getUUIDs, putUUIDs}
import io.github.jugbot.util.{memoizedValue, BlockStateRequirement}
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

import java.util.UUID
import java.util.function.Supplier
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.StreamConverters.*

class ShrineBlockEntity(pos: BlockPos, state: BlockState)
    extends BlockEntity(ShrineBlockEntity.TYPE.get(), pos, state) {
  var tier: Int = 0
  var nextTierRequirements: Set[BlockStateRequirement] = Set.empty
  var owners = mutable.Set.empty[UUID]

  val getShrineZone: () => Option[ShrineZoneEntity] = memoizedValue(
    () => this.getLevel.getEntitiesOfClass(classOf[ShrineZoneEntity], AABB(this.getBlockPos)).asScala.headOption,
    (zone: Option[ShrineZoneEntity]) => zone.exists(_.isAlive)
  )

  // TODO: Move this into shrine zone and have it modify the zone's tier... what if the zone is destroyed? Tier lives in zone instead?
  private def getUnsatisfiedRequirements[T](level: Level,
                                            pos: BlockPos,
                                            requirements: Set[BlockStateRequirement]
  ): Set[BlockStateRequirement] = {
    val blocks = getShrineZone() match {
      case Some(e) =>
        level
          .getBlockStates(
            e.getBoundingBox
          )
          .toScala(LazyList)
          .toList
      case None => List.empty
    }
    requirements.map(req => blocks.foldLeft(req)((acc, bs) => acc.tally(bs)))
  }

  private val TAG_TIER = "tier"
  private val TAG_OWNERS = "owners"

  override def load(compoundTag: CompoundTag): Unit =
    super.load(compoundTag)
    tier = compoundTag.getInt(TAG_TIER)
    owners = mutable.Set.empty ++= (compoundTag.getUUIDs(TAG_OWNERS))

  override def saveAdditional(compoundTag: CompoundTag): Unit = {
    super.saveAdditional(compoundTag)
    compoundTag.putInt(TAG_TIER, tier)
    compoundTag.putUUIDs(TAG_OWNERS, owners)
  }

}

object ShrineBlockEntity {

  final val TYPE: Supplier[BlockEntityType[ShrineBlockEntity]] = Suppliers.memoize(() =>
    BlockEntityType.Builder
      .of[ShrineBlockEntity](new ShrineBlockEntity(_, _), ShrineBlock.INSTANCE)
      // TODO: what should this be if not null?
      .build(null)
  )

  // TODO: "Disabled" state if no zone exists..?

  // TODO: Make the requirement check async since more than one shrine causes large performance dips
  def tick(level: Level, pos: BlockPos, blockState: BlockState, unknownBlockEntity: BlockEntity): Unit = {
    if !unknownBlockEntity.isInstanceOf[ShrineBlockEntity] then return
    val shrineBlockEntity = unknownBlockEntity.asInstanceOf[ShrineBlockEntity]
    val unsatisfiedRequirements =
      shrineTierRequirements.map(reqs => shrineBlockEntity.getUnsatisfiedRequirements(level, pos, reqs))
    val maybeNextTierIndex =
      unsatisfiedRequirements.indexWhere(requirements => requirements.exists(req => !req.isFullySatisfied))
    val nextTier = if maybeNextTierIndex == -1 then unsatisfiedRequirements.length else maybeNextTierIndex
    val nextRequirements =
      if maybeNextTierIndex == -1 then Set.empty[BlockStateRequirement] else unsatisfiedRequirements(maybeNextTierIndex)

    shrineBlockEntity.tier = nextTier
    shrineBlockEntity.nextTierRequirements = nextRequirements
  }

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
