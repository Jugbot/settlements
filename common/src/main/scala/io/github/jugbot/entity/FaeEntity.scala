package io.github.jugbot.entity

import com.google.common.base.Suppliers
import io.github.jugbot.ai.{runModules, BehaviorFailure, BehaviorRunning, BehaviorStatus, BehaviorSuccess}
import io.github.jugbot.ai.tree.{BlackboardKey, FaeBehavior, FaeBehaviorTree}
import net.minecraft.core.BlockPos
import net.minecraft.nbt.{CompoundTag, NbtUtils}
import net.minecraft.network.protocol.game.DebugPackets
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.{AttributeSupplier, Attributes}
import net.minecraft.world.entity.ai.village.poi.{PoiManager, PoiTypes}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.state.BlockState

import java.util as ju
import java.util.Optional
import java.util.function.Supplier

class FaeEntity(entityType: EntityType[FaeEntity], world: Level) extends Mob(entityType, world) {

  override def getArmorSlots: java.lang.Iterable[ItemStack] = ju.List.of()

  override def getItemBySlot(equipmentSlot: EquipmentSlot): ItemStack =
    ItemStack.EMPTY

  override def setItemSlot(
    equipmentSlot: EquipmentSlot,
    itemStack: ItemStack
  ): Unit = {}

  override def isPushable: Boolean = false

  override def doPush(entity: Entity): Unit = {}

  override def tick(): Unit = {
    super.tick()
    if this.level().isClientSide then {
      return
    }
    runModules(
      FaeBehaviorTree.map.getOrElse("root", FaeBehaviorTree.fallback),
      this.performBehavior,
      FaeBehaviorTree.map
    )
  }

  private def getBlackboard(key: String): Option[Any] =
    key match {
      case BlackboardKey.bed_position => this.bedPosition
      // TODO: This might be the wrong signal to give when a key name is unexpected
      case _ => None
    }

  private def performBehavior(name: String, args: Map[String, String]): BehaviorStatus =
    val maybeBehavior = FaeBehavior.valueOf(name, args)
    if maybeBehavior.isEmpty then
      throw new Exception(s"Encountered unknown behavior: $name with $args")
      return BehaviorFailure
    maybeBehavior.get match {
      case FaeBehavior.unknown() =>
        throw new Exception("Encountered an unknown behavior. There is likely a problem with your config.")
        BehaviorFailure
      case FaeBehavior.unimplemented() =>
        BehaviorSuccess
      case FaeBehavior.is_tired() =>
        if this.level().isNight then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.has(key) =>
        getBlackboard(key) match {
          case Some(_) => BehaviorSuccess
          case _       => BehaviorFailure
        }
      case FaeBehavior.sleep() =>
        if this.bedPosition.isDefined then
          this.startSleeping(this.bedPosition.get)
          BehaviorSuccess
        else BehaviorFailure
      case FaeBehavior.bed_is_valid() =>
        val blockState: BlockState = this.level().asInstanceOf[ServerLevel].getBlockState(bedPosition.get)
        if blockState.is(BlockTags.BEDS) && blockState
            .getValue(BedBlock.OCCUPIED) == false
        then BehaviorSuccess
        else BehaviorFailure
      case FaeBehavior.claim_bed() =>
        val poiManager = this.level().asInstanceOf[ServerLevel].getPoiManager
        // TODO: instead of finding beds within a distance we should keep record of all beds within the kingdom
        val takenPOI = poiManager.take(holder => holder.is(PoiTypes.HOME), (_, _) => true, this.blockPosition, 32)
        bedPosition = if takenPOI.isPresent then Some(takenPOI.get) else None
        bedPosition match {
          case Some(value) => BehaviorSuccess
          case None        => BehaviorFailure
        }
      case FaeBehavior.is_at_location(key) =>
        getBlackboard(key) match {
          case Some(blockPos: BlockPos)
              if this.getY > blockPos.getY.toDouble + 0.4 && blockPos.closerToCenterThan(this.position, 1.14) =>
            BehaviorSuccess
          case _ => BehaviorFailure
        }
      case FaeBehavior.has_nav_path_to(key) =>
        getBlackboard(key) match {
          case Some(blockPos: BlockPos)
              if this.getNavigation.getTargetPos != null && blockPos.distManhattan(
                this.getNavigation.getTargetPos
              ) <= 1 && this.getNavigation.isInProgress =>
            BehaviorSuccess
          case _ => BehaviorFailure
        }
      case FaeBehavior.create_nav_path_to(key) =>
        getBlackboard(key) match {
          case Some(blockPos: BlockPos) =>
            val path = this.getNavigation.createPath(blockPos, 1)
            if this.getNavigation.moveTo(path, 1) then BehaviorSuccess else BehaviorFailure
          case _ => BehaviorFailure
        }
      case FaeBehavior.current_path_unobstructed() =>
        if this.getNavigation.isStuck then BehaviorFailure else BehaviorSuccess
      case FaeBehavior.move_along_current_path() =>
        val nav = this.getNavigation
        if nav.isInProgress then
          nav.tick()
          BehaviorRunning
        else BehaviorSuccess
    }

  override def die(damageSource: DamageSource): Unit = {
    super.die(damageSource)
    if this.level().isClientSide then {
      return
    }
    val serverLevel = this.level().asInstanceOf[ServerLevel]

    val poiManager: PoiManager = serverLevel.getPoiManager
    val optional = bedPosition match {
      case Some(blockPos) => poiManager.getType(blockPos)
      case None           => Optional.empty()
    }
    if optional.isPresent then {
      poiManager.release(bedPosition.get)
      // TODO: Copy-pasta, what is this?
      DebugPackets.sendPoiTicketCountPacket(serverLevel, bedPosition.get)
    }
  }

  private var bedPosition: Option[BlockPos] = Option.empty

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    super.addAdditionalSaveData(compoundTag)
    bedPosition.foreach(pos => compoundTag.put(FaeEntity.BED_POSITION_NBT_KEY, NbtUtils.writeBlockPos(bedPosition.get)))
  }

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    super.readAdditionalSaveData(compoundTag)
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
  private val BED_POSITION_NBT_KEY = "bedPosition"

  final val TYPE: Supplier[EntityType[FaeEntity]] = Suppliers.memoize(() =>
    EntityType.Builder
      .of[FaeEntity](new FaeEntity(_, _), MobCategory.MISC)
      .sized(0.6f, 1.8f)
      .build("fae_entity")
  )

  def createAttributes(): AttributeSupplier.Builder =
    LivingEntity
      .createLivingAttributes()
      .add(Attributes.MAX_HEALTH, 10.0)
      .add(Attributes.MOVEMENT_SPEED, 1.0f)
      .add(Attributes.FOLLOW_RANGE, 20f)

}
