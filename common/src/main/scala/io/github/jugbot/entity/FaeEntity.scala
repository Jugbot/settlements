package io.github.jugbot.entity

import com.google.common.base.Suppliers
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

import java.lang
import java.util.function.Supplier
import java.util as ju
import io.github.jugbot.ai.state
import io.github.jugbot.ai.Node
import io.github.jugbot.ai.ActionNode
import io.github.jugbot.ai.Failure
import io.github.jugbot.ai.Success
import io.github.jugbot.ai.tree.FaeBehavior
import io.github.jugbot.ai.Status
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.BlockPos
import net.minecraft.nbt.NbtUtils
import io.github.jugbot.ai.tree.FaeBehaviorTree
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.village.poi.PoiTypes
import net.minecraft.world.entity.ai.village.poi.PoiManager
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.BedBlock

class FaeEntity(entityType: EntityType[? <: LivingEntity], world: Level) extends LivingEntity(entityType, world) {

  override def getArmorSlots(): java.lang.Iterable[ItemStack] = ju.List.of()

  override def getItemBySlot(equipmentSlot: EquipmentSlot): ItemStack =
    ItemStack.EMPTY

  override def setItemSlot(
    equipmentSlot: EquipmentSlot,
    itemStack: ItemStack
  ): Unit = {}

  override def getMainArm(): HumanoidArm = HumanoidArm.RIGHT

  override def isPushable(): Boolean = false

  override def doPush(entity: Entity) = {}

  override def attackable(): Boolean = false

  override def isEffectiveAi(): Boolean = false

  override def baseTick(): Unit = {
    super.baseTick()
    state(
      FaeEntity.behaviorTree,
      (behavior: FaeBehavior) =>
        behavior match {
          case FaeBehavior.unknown => {
            println("Encountered a behavior without an implementation!")
            Failure
          }
          case FaeBehavior.unimplemented => {
            println("Encountered unimplemented behavior, skipping.")
            Success
          }
          case FaeBehavior.is_tired => {
            if this.level().isNight() then Success else Failure
          }
          case FaeBehavior.has_valid_bed => {
            bedPosition match {
              case None => Failure
              case Some(blockPos) => {
                val blockState: BlockState = this.level().asInstanceOf[ServerLevel].getBlockState(blockPos);
                if blockPos.closerToCenterThan(this.position(), 2.0) && blockState.is(BlockTags.BEDS) && blockState
                    .getValue(BedBlock.OCCUPIED) == false
                then Success
                else Failure
              }
            }
          }
          case FaeBehavior.sleep => {
            if this.bedPosition.isDefined then Success else Failure
          }
          case FaeBehavior.claim_bed => {
            val poiManager = this.level().asInstanceOf[ServerLevel].getPoiManager()
            // TODO: instead of finding beds within a distance we should keep record of all beds within the kingdom
            val maybeHome = poiManager.findClosest(holder => holder.is(PoiTypes.HOME),
                                                   this.blockPosition(),
                                                   48,
                                                   PoiManager.Occupancy.HAS_SPACE
            )
            if maybeHome.isPresent() then {
              val takenPOI = poiManager.take(holder => holder.is(PoiTypes.HOME), (_, _) => true, maybeHome.get, 32)
              bedPosition = if takenPOI.isPresent() then Some(takenPOI.get) else None
              bedPosition match {
                case Some(value) => Success
                case None        => Failure
              }
            } else Failure
          }
        }
    )
  }

  var bedPosition: Option[BlockPos] = Option.empty

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    super.addAdditionalSaveData(compoundTag);
    bedPosition.foreach(pos => compoundTag.put(FaeEntity.BED_POSITION_NBT_KEY, NbtUtils.writeBlockPos(bedPosition.get)))
  }

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    super.readAdditionalSaveData(compoundTag);
    if compoundTag.contains(FaeEntity.BED_POSITION_NBT_KEY) then {
      this.bedPosition = Some(
        NbtUtils.readBlockPos(
          compoundTag.getCompound(FaeEntity.BED_POSITION_NBT_KEY)
        )
      )
    }
  }
}

object FaeEntity {
  val BED_POSITION_NBT_KEY = "bedPosition"

  final val TYPE: Supplier[EntityType[FaeEntity]] = Suppliers.memoize(() =>
    EntityType.Builder
      .of[FaeEntity](new FaeEntity(_, _), MobCategory.MISC)
      .sized(0.98f, 0.7f)
      .build("fae_entity")
  );

  def createAttributes(): AttributeSupplier.Builder = {
    LivingEntity
      .createLivingAttributes()
      .add(Attributes.MAX_HEALTH, 10.0)
      .add(Attributes.MOVEMENT_SPEED, 0.2f)
  }

  val behaviorTree: Node[FaeBehavior] = FaeBehaviorTree.root
}
