package io.github.jugbot.entity

import com.google.common.base.Suppliers
import com.mojang.brigadier.StringReader
import io.github.jugbot
import io.github.jugbot.ai.*
import io.github.jugbot.ai.tree.{FaeBehavior, FaeBehaviorTree}
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument
import net.minecraft.commands.arguments.item.ItemPredicateArgument
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.nbt.{CompoundTag, NbtUtils}
import net.minecraft.network.protocol.game.DebugPackets
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.{AttributeSupplier, Attributes}
import net.minecraft.world.entity.ai.village.poi.{PoiManager, PoiTypes}
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.{BlockItem, ItemStack}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.entity.{BlockEntityType, ChestBlockEntity}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.{Container, InteractionHand, SimpleContainer}

import java.util.function.Supplier
import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.OptionConverters.RichOptional
import io.github.jugbot.extension.Container.*
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.gameevent.GameEvent.Context

class FaeEntity(entityType: EntityType[FaeEntity], world: Level) extends InventoryMob(entityType, world) {

  override def isPushable: Boolean = false

  override def doPush(entity: Entity): Unit = {}

  override def canPickUpLoot = true

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

  private object SPECIAL_KEYS {
    val TARGET = "target"
    val BED_POSITION = "bed_position"
  }

  private val blackboard = mutable.Map.empty[String, Any]

  private def performBehavior(name: String, args: Map[String, String]): BehaviorStatus =
    val maybeBehavior = FaeBehavior.valueOf(name, args)
    if maybeBehavior.isEmpty then throw new Exception(f"Encountered unknown behavior: $name with $args")
    val profiler = this.level().getProfiler
    profiler.push(name)
    val status = maybeBehavior.get match {
      case FaeBehavior.unknown() =>
        throw new Exception("Encountered an unknown behavior. There is likely a problem with your config.")
      case FaeBehavior.unimplemented() =>
        BehaviorSuccess
      case FaeBehavior.is_tired() =>
        if this.level().isNight then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.has(key) =>
        blackboard.get(key) match {
          case Some(_) => BehaviorSuccess
          case _       => BehaviorFailure
        }
      case FaeBehavior.sleep() =>
        blackboard.get(SPECIAL_KEYS.BED_POSITION) match {
          case Some(bedPosition: BlockPos) =>
            this.startSleeping(bedPosition)
            // Return running here so that follow up tasks are not executed
            BehaviorRunning
          case _ => BehaviorFailure
        }
      case FaeBehavior.bed_is_valid() =>
        blackboard.get(SPECIAL_KEYS.BED_POSITION) match {
          case Some(bedPosition: BlockPos) =>
            val poiManager = this.level().asInstanceOf[ServerLevel].getPoiManager
            val blockState: BlockState = this.level().asInstanceOf[ServerLevel].getBlockState(bedPosition)
            if blockState.is(BlockTags.BEDS)
            then BehaviorSuccess
            else BehaviorFailure
          case _ => BehaviorFailure
        }
      case FaeBehavior.claim_bed() =>
        val poiManager = this.level().asInstanceOf[ServerLevel].getPoiManager
        // TODO: instead of finding beds within a distance we should keep record of all beds within the kingdom
        val takenPOI = poiManager.take(
          holder => holder.is(PoiTypes.HOME),
          (_, bp) => {
            val blockState: BlockState = this.level().asInstanceOf[ServerLevel].getBlockState(bp)
            blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED) == false
          },
          this.blockPosition,
          32
        )
        if takenPOI.isPresent then
          blackboard.update(SPECIAL_KEYS.BED_POSITION, takenPOI.get)
          BehaviorSuccess
        else BehaviorFailure
      case FaeBehavior.is_at_location(key) =>
        blackboard.get(key) match {
          case Some(blockPos: BlockPos)
              if this.getY + 0.4 > blockPos.getY.toDouble && blockPos.closerToCenterThan(this.position, 1.5) =>
            BehaviorSuccess
          case _ => BehaviorFailure
        }
      case FaeBehavior.has_nav_path_to(key) =>
        blackboard.get(key) match {
          case Some(blockPos: BlockPos)
              if this.getNavigation.getTargetPos != null && blockPos.distManhattan(
                this.getNavigation.getTargetPos
              ) <= 1 && this.getNavigation.isInProgress =>
            BehaviorSuccess
          case _ => BehaviorFailure
        }
      case FaeBehavior.create_nav_path_to(key) =>
        blackboard.get(key) match {
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
      case FaeBehavior.is_sleeping() =>
        if this.isSleeping then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.is_not_sleeping() =>
        if !this.isSleeping then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.stop_sleeping() =>
        this.stopSleeping()
        BehaviorSuccess
      case FaeBehavior.target_closest_block(blockQuery) =>
        val block = parseBlockPredicate(blockQuery)
        val maybeClosest = BlockPos
          .findClosestMatch(this.blockPosition,
                            12,
                            12,
                            bp =>
                              block.test(
                                BlockInWorld(this.level(), bp, false)
                              )
          )
          .toScala
        maybeClosest match {
          case Some(value) =>
            blackboard.update("target", value)
            BehaviorSuccess
          case None => BehaviorFailure
        }
      case FaeBehavior.has_space_for_target_produce() =>
        blackboard.get(SPECIAL_KEYS.TARGET) match {
          case Some(blockPos: BlockPos) =>
            val items = this
              .level()
              .getBlockState(blockPos)
              .getDrops(
                LootParams
                  .Builder(this.level.asInstanceOf[ServerLevel])
                  .withParameter(LootContextParams.ORIGIN, this.position())
                  .withParameter(LootContextParams.TOOL, this.getItemInHand(InteractionHand.MAIN_HAND))
                  .withParameter(LootContextParams.THIS_ENTITY, this)
              )
              .asScala
            // TODO: See if you can pick up all items not just individuals
            if items.forall(item => canTakeItem(item)) then BehaviorSuccess else BehaviorFailure
          case _ => BehaviorFailure
        }
      case FaeBehavior.break_block(target) =>
        blackboard.get(SPECIAL_KEYS.TARGET) match {
          // TODO: Use player break animation/duration/tool
          case Some(blockPos: BlockPos) if this.level().destroyBlock(blockPos, true, this) =>
            BehaviorSuccess
          case _ =>
            BehaviorFailure
        }
      case FaeBehavior.place_item_on_block(itemQuery, blockPos, side) =>
        // TODO: Investigate making item usage more generic and based on player behavior Block.use or Item.useOn
        blackboard.get(blockPos) match {
          case Some(blockPos: BlockPos) if this.equipFromInventory(itemQuery, EquipmentSlot.MAINHAND) =>
            val itemStack = this.getItemInHand(InteractionHand.MAIN_HAND)
            if !itemStack.isEmpty then
              val item = itemStack.getItem
              item match {
                case blockItem: BlockItem =>
                  // based on HarvestFarmland.class
                  val blockState = blockItem.getBlock.defaultBlockState()
                  val relativePos = blockPos.relative(Direction.byName(side))
                  this.level.setBlockAndUpdate(relativePos, blockState)
//                  this.level.asInstanceOf[ServerLevel].gameEvent(GameEvent.BLOCK_PLACE, relativePos, GameEvent.Context.of (this, blockState) )
                  // TODO: PLay sound?
                  itemStack.shrink(1)
                  if (itemStack.isEmpty) this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
                  BehaviorSuccess
                case _ => BehaviorFailure
              }
            else
              BehaviorFailure
          case _ =>
            BehaviorFailure
        }
      case FaeBehavior.holds_at_least(itemQuery, amount) =>
        if count(items)(itemQuery) >= amount.toInt then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.holds_at_most(itemQuery, amount) =>
        if count(items)(itemQuery) <= amount.toInt then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.target_nearest_stockpile_with(itemQuery) =>
        val itemTester = parseItemPredicate(itemQuery)
        val maybeClosest = BlockPos
          .findClosestMatch(this.blockPosition,
                            12,
                            12,
                            bp => this.level.getBlockEntity(bp, BlockEntityType.CHEST).isPresent
          )
          .toScala
        maybeClosest match {
          case Some(blockPos) =>
            val chest = this.level.getBlockEntity(blockPos, BlockEntityType.CHEST).get
            chest.items.count(itemStack => itemTester.test(itemStack)) match {
              case 0 => BehaviorFailure
              case _ =>
                blackboard.update(SPECIAL_KEYS.TARGET, blockPos)
                BehaviorSuccess
            }
          case None => BehaviorFailure
        }
      case FaeBehavior.transfer_item_from_target_until(itemQuery, amount) =>
        blackboard.get(SPECIAL_KEYS.TARGET).flatMap { case bp: BlockPos =>
          this.level.getBlockEntity(bp, BlockEntityType.CHEST).toScala
        } match
          case Some(chest: ChestBlockEntity) =>
            val success = transferItemsUntil(chest, this, itemQuery, amount.toInt)
            if success then BehaviorSuccess else BehaviorFailure
          case None => BehaviorFailure
      case FaeBehavior.transfer_item_to_target_until(itemQuery, amount) =>
        blackboard.get(SPECIAL_KEYS.TARGET).flatMap { case bp: BlockPos =>
          this.level.getBlockEntity(bp, BlockEntityType.CHEST).toScala
        } match
          case Some(chest: ChestBlockEntity) =>
            val success = transferItemsUntil(this, chest, itemQuery, amount.toInt)
            if success then BehaviorSuccess else BehaviorFailure
          case None => BehaviorFailure
      case FaeBehavior.set(to, from) =>
        blackboard.get(from) match {
          case Some(value) => blackboard.update(to, value)
          case None        => blackboard.remove(to)
        }
        BehaviorSuccess
      case FaeBehavior.holds(itemQuery, min, max) =>
        val count = this.count(items)(itemQuery)
        if count >= min.toInt && count <= max.toInt then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.obtain_job() =>
        blackboard.update("job", "farmer")
        BehaviorSuccess
      case FaeBehavior.equals_literal(key, value) =>
        blackboard.get(key) match {
          case Some(v) if v == value => BehaviorSuccess
          case _                     => BehaviorFailure
        }
    }
    profiler.pop()
    status

  override def die(damageSource: DamageSource): Unit = {
    super.die(damageSource)
    if this.level().isClientSide then {
      return
    }
    val serverLevel = this.level().asInstanceOf[ServerLevel]

    val poiManager: PoiManager = serverLevel.getPoiManager
    blackboard.get(SPECIAL_KEYS.BED_POSITION).foreach { case Some(blockPos: BlockPos) =>
      val optional = poiManager.getType(blockPos)
      if optional.isPresent then {
        poiManager.release(blockPos)
        // TODO: Copy-pasta, what is this?
        DebugPackets.sendPoiTicketCountPacket(serverLevel, blockPos)
      }
    }
  }

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    super.addAdditionalSaveData(compoundTag)
    blackboard.get(SPECIAL_KEYS.BED_POSITION).foreach {
      case Some(pos: BlockPos) => compoundTag.put(FaeEntity.BED_POSITION_NBT_KEY, NbtUtils.writeBlockPos(pos))
      case _                   => ()
    }
  }

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    super.readAdditionalSaveData(compoundTag)
    if compoundTag.contains(FaeEntity.BED_POSITION_NBT_KEY) then {
      blackboard.update(
        SPECIAL_KEYS.BED_POSITION,
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
      .add(Attributes.MOVEMENT_SPEED, 0.3f)
      .add(Attributes.FOLLOW_RANGE, 20f)

}
