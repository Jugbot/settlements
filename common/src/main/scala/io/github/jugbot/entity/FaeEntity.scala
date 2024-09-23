package io.github.jugbot.entity

import com.google.common.base.Suppliers
import io.github.jugbot.ai.*
import io.github.jugbot.ai.tree.{FaeBehavior, FaeBehaviorTree}
import io.github.jugbot.extension.AABB.*
import io.github.jugbot.extension.BoundingBox.*
import io.github.jugbot.extension.Container.*
import io.github.jugbot.extension.Container.Query.*
import io.github.jugbot.util.{blockPredicate, itemPredicate}
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.{CompoundTag, NbtUtils}
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.DebugPackets
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.tags.BlockTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.{AttributeSupplier, Attributes}
import net.minecraft.world.entity.ai.village.poi.{PoiManager, PoiTypes}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{BlockItem, ItemStack}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.entity.{BlockEntityType, ChestBlockEntity}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.{InteractionHand, InteractionResult}

import java.util.function.Supplier
import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.OptionConverters.RichOptional
import io.github.jugbot.entity.zone.SettlementZoneEntity

class FaeEntity(entityType: EntityType[FaeEntity], world: Level)
    extends Mob(entityType: EntityType[FaeEntity], world: Level)
    with ExtraInventory
    with Hunger
    with OwnedBy[SettlementZoneEntity, FaeEntity] {

  override def isPushable: Boolean = false

  override def doPush(entity: Entity): Unit = {}

  override def canPickUpLoot = true

  override def shouldShowName = false

  private var behaviorLog = Seq.empty[BehaviorLog]
  override def tick(): Unit = {
    super.tick()
    if this.level().isClientSide then {
      return
    }
    behaviorLog = runModules(
      FaeBehaviorTree.map.getOrElse("root", FaeBehaviorTree.fallback),
      this.debugWrapper,
      FaeBehaviorTree.map
    )
    ()
  }

  private object SPECIAL_KEYS {
    val TARGET = "target"
    val BED_POSITION = "bed_position"
  }

  private val blackboard = mutable.Map.empty[String, Any]

  private def debugWrapper(name: String, args: Map[String, String]): BehaviorStatus =
    val profiler = this.level().getProfiler
    profiler.push(name)
    val result = performBehavior(name, args)
    profiler.pop()
    result

  private def performBehavior(name: String, args: Map[String, String]): BehaviorStatus =
    val maybeBehavior = FaeBehavior.valueOf(name, args)
    maybeBehavior.get match {
      case FaeBehavior.unknown() =>
        throw new Exception(
          "Encountered an unknown behavior. There is likely a problem with your config. See warnings above."
        )
      case FaeBehavior.unimplemented() =>
        BehaviorSuccess
      case FaeBehavior.failure() =>
        BehaviorFailure
      case FaeBehavior.success() =>
        BehaviorSuccess
      case FaeBehavior.running() =>
        BehaviorRunning
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
              if blockPos.closerToCenterThan(this.position, FaeEntity.NAVIGATION_PROXIMITY + 1) =>
            BehaviorSuccess
          case _ => BehaviorFailure
        }
      case FaeBehavior.nav_ended() =>
        if this.getNavigation.isDone then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.reset_nav() =>
        this.getNavigation.stop()
        BehaviorSuccess
      case FaeBehavior.resolve_nav() =>
        val success = this.getNavigation.getPath.canReach
        this.getNavigation.stop()
        if success then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.has_nav_path_to(key) =>
        val currentTarget = Option(this.getNavigation.getPath).map(path => path.getTarget)
        (blackboard.get(key), currentTarget) match {
          case (Some(posA: BlockPos), Some(posB: BlockPos))
              if posA.distManhattan(posB) <= FaeEntity.NAVIGATION_PROXIMITY =>
            BehaviorSuccess
          case _ => BehaviorFailure
        }
      case FaeBehavior.create_nav_path_to(key) =>
        blackboard.get(key) match {
          case Some(blockPos: BlockPos) =>
            this.getNavigation.stop()
            val path = this.getNavigation.createPath(blockPos, FaeEntity.NAVIGATION_PROXIMITY)
            if path != null && path.canReach && this.getNavigation.moveTo(path, 1)
            then BehaviorSuccess
            else BehaviorFailure
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
        val predicate = blockPredicate(blockQuery)
        val maybeClosest = this.bruteForceSearch(bp =>
          predicate(
            BlockInWorld(this.level(), bp, false)
          )
        )
        maybeClosest match {
          case Some(value) =>
            blackboard.update("target", value)
            BehaviorSuccess
          case None => BehaviorFailure
        }
      case FaeBehavior.target_is_block(blockQuery) =>
        blackboard.get(SPECIAL_KEYS.TARGET) match {
          case Some(blockPos: BlockPos) =>
            val predicate = blockPredicate(blockQuery)
            if predicate(BlockInWorld(this.level(), blockPos, false)) then BehaviorSuccess else BehaviorFailure
          case _ => BehaviorFailure
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
      case FaeBehavior.place_item_at_target(itemQuery, blockPos) =>
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
                  this.level.setBlockAndUpdate(blockPos, blockState)
                  val serverLevel = this.level.asInstanceOf[ServerLevel]
                  serverLevel.gameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Context.of(this, blockState))
                  serverLevel.playSound(null.asInstanceOf[Player],
                                        blockPos.getX.toDouble,
                                        blockPos.getY.toDouble,
                                        blockPos.getZ.toDouble,
                                        SoundEvents.CROP_PLANTED,
                                        SoundSource.BLOCKS,
                                        1.0f,
                                        1.0f
                  )
                  itemStack.shrink(1)
                  if itemStack.isEmpty then this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
                  BehaviorSuccess
                case _ => BehaviorFailure
              }
            else BehaviorFailure
          case _ =>
            BehaviorFailure
        }
      case FaeBehavior.holds_at_least(itemQuery, amount) =>
        if this.getInventory.count(itemQuery) >= amount.toInt then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.holds_at_most(itemQuery, amount) =>
        if this.getInventory.count(itemQuery) <= amount.toInt then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.target_nearest_stockpile_with(itemQuery) =>
        val predicate = itemPredicate(itemQuery)
        val maybeClosest =
          this.bruteForceSearch { (bp: BlockPos) =>
            this.level.getBlockEntity(bp, BlockEntityType.CHEST).toScala match {
              case Some(chest: ChestBlockEntity) => chest.items.count(predicate) > 0
              case _                             => false
            }
          }
        maybeClosest match {
          case Some(blockPos) =>
            blackboard.update(SPECIAL_KEYS.TARGET, blockPos)
            BehaviorSuccess
          case None => BehaviorFailure
        }
      case FaeBehavior.transfer_item_from_target_until(itemQuery, amount) =>
        blackboard.get(SPECIAL_KEYS.TARGET).flatMap { case bp: BlockPos =>
          this.level.getBlockEntity(bp, BlockEntityType.CHEST).toScala
        } match
          case Some(chest: ChestBlockEntity) =>
            val success = chest.transferItemsUntilTargetHas(this.getInventory, itemQuery, amount.toInt)
            if success then BehaviorSuccess else BehaviorFailure
          case None => BehaviorFailure
      case FaeBehavior.transfer_item_to_target_until(itemQuery, amount) =>
        blackboard.get(SPECIAL_KEYS.TARGET).flatMap { case bp: BlockPos =>
          this.level.getBlockEntity(bp, BlockEntityType.CHEST).toScala
        } match
          case Some(chest: ChestBlockEntity) =>
            val success = this.getInventory.transferItemsUntilSelfHas(chest, itemQuery, amount.toInt)
            if success then BehaviorSuccess else BehaviorFailure
          case None => BehaviorFailure
      case FaeBehavior.set(to, from) =>
        blackboard.get(from) match {
          case Some(value) => blackboard.update(to, value)
          case None        => blackboard.remove(to)
        }
        BehaviorSuccess
      case FaeBehavior.remove(key) =>
        blackboard.remove(key)
        BehaviorSuccess
      case FaeBehavior.holds(itemQuery, min, max) =>
        val count = this.getInventory.count(itemQuery)
        if count >= min.toInt && count <= max.toInt then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.obtain_job() =>
        blackboard.update("job", "farmer_wheat")
        BehaviorSuccess
      case FaeBehavior.equals_literal(key, value) =>
        blackboard.get(key) match {
          case Some(v) if v == value => BehaviorSuccess
          case _                     => BehaviorFailure
        }
      case FaeBehavior.is_hungry() =>
        if this.foodData.needsFood then BehaviorSuccess else BehaviorFailure
      case FaeBehavior.eat_food() =>
        this.getInventory.queryFirst("#c:foods") match {
          case Some(ItemSlot(itemStack, slot)) =>
            val newStack = this.foodData.eat(itemStack)
            this.getInventory.setItem(slot, newStack)
            BehaviorSuccess
          case None => BehaviorFailure
        }
      case FaeBehavior.add(key, value) =>
        blackboard.get(key) match {
          case Some(acc: mutable.Set[?]) =>
            acc.asInstanceOf[mutable.Set[String]].add(value)
          case _ =>
            val t = mutable.Set(value)
            blackboard.update(key, t)
        }
        BehaviorSuccess
    }

  override def mobInteract(player: Player, interactionHand: InteractionHand): InteractionResult = {
    if player.isCrouching && !player.isLocalPlayer then {
      player.sendSystemMessage(Component.literal("Behavior Log:").withStyle(ChatFormatting.BOLD))
      behaviorLog.foreach { log =>
        val basicMessage = Component.literal(log.toString())
        val styledMessage =
          if log.isModule then basicMessage.withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD)
          else basicMessage.withStyle(ChatFormatting.BLUE)
        player.sendSystemMessage(styledMessage)
      }
    }
    super.mobInteract(player, interactionHand)
  }

  override def die(damageSource: DamageSource): Unit = {
    super.die(damageSource)
    if this.level().isClientSide then {
      return
    }
    val serverLevel = this.level().asInstanceOf[ServerLevel]

    val poiManager: PoiManager = serverLevel.getPoiManager
    blackboard.get(SPECIAL_KEYS.BED_POSITION).foreach {
      case Some(blockPos: BlockPos) =>
        val optional = poiManager.getType(blockPos)
        if optional.isPresent then {
          poiManager.release(blockPos)
          // TODO: Copy-pasta, what is this?
          DebugPackets.sendPoiTicketCountPacket(serverLevel, blockPos)
        }
      case _ => ()
    }
  }

  private val settlementZone = parent

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

  /**
   * Attempts to search for a block within a radius.
   * TODO: Instead of fixed & random, consider nonlinear random + caching
   */
  private def bruteForceSearch(predicate: Function[BlockPos, Boolean]): Option[BlockPos] =
    this.blackboard.get(SPECIAL_KEYS.TARGET) match {
      // Shortcut if there is already a valid target
      case Some(bp: BlockPos) if predicate(bp) => Option(bp)
      // Else perform expensive search
      case _ =>
        // Check adjacent blocks first, then do a limited random search in a large area
        // The adjacent search should not go beyond the navigation termination distance otherwise the pathfinding could get stuck on a bad target
        settlementZone.flatMap(
          _.getBoundingBox.toBoundingBox.closestCoordinatesInside(this.blockPosition).map(BlockPos(_)).find(predicate)
        )
    }
}

object FaeEntity {
  private val BED_POSITION_NBT_KEY = "bedPosition"

  private val NAVIGATION_PROXIMITY = 1
  private val BRUTE_FORCE_SEARCH_RADIUS = 12
  private val BRUTE_FORCE_SEARCH_ATTEMPTS = 20

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
