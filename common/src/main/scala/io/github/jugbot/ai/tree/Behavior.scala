package io.github.jugbot.ai.tree

import io.github.jugbot.ai.{BehaviorFailure, BehaviorRunning, BehaviorStatus, BehaviorSuccess}
import io.github.jugbot.entity.{FaeEntity, RandomTickingEntity}
import io.github.jugbot.extension.Container.*
import io.github.jugbot.extension.Container.Query.*
import io.github.jugbot.util.{blockPredicate, itemPredicate}
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ai.village.poi.PoiTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.{EquipmentSlot, Mob}
import net.minecraft.world.item.{BlockItem, ItemStack}
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.entity.{BlockEntityType, ChestBlockEntity}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

private object SPECIAL_KEYS {
  val TARGET = "target"
  val BED_POSITION = "bed_position"
}

type Blackboard = mutable.Map[String, Any]

sealed trait Behavior[-Actor, -Blackboard] {
  def execute(actor: Actor, state: Blackboard): BehaviorStatus
}

sealed trait ConstantBehavior extends Behavior[Any, Any]:
  override def execute(actor: Any, state: Any): BehaviorStatus = execute()
  def execute(): BehaviorStatus

object ConstantBehavior:
  case class failure() extends ConstantBehavior:
    override def execute(): BehaviorStatus = BehaviorFailure

  case class success() extends ConstantBehavior:
    override def execute(): BehaviorStatus = BehaviorSuccess

  case class running() extends ConstantBehavior:
    override def execute(): BehaviorStatus = BehaviorRunning

sealed trait NavigationBehavior extends Behavior[Mob, Blackboard]

object NavigationBehavior:
  case class is_at_location(target: String) extends NavigationBehavior:
    override def execute(actor: Mob, state: Blackboard): BehaviorStatus =
      state.get(target) match {
        case Some(blockPos: BlockPos)
            if blockPos.closerToCenterThan(actor.position, FaeEntity.NAVIGATION_PROXIMITY + 1) =>
          BehaviorSuccess
        case _ => BehaviorFailure
      }

  case class nav_ended() extends NavigationBehavior:
    override def execute(actor: Mob, state: Blackboard): BehaviorStatus =
      if actor.getNavigation.isDone then BehaviorSuccess else BehaviorFailure

  case class reset_nav() extends NavigationBehavior:
    override def execute(actor: Mob, state: Blackboard): BehaviorStatus =
      actor.getNavigation.stop()
      BehaviorSuccess
  case class resolve_nav() extends NavigationBehavior:
    override def execute(actor: Mob, state: Blackboard): BehaviorStatus =
      val success = actor.getNavigation.getPath.canReach
      actor.getNavigation.stop()
      if success then BehaviorSuccess else BehaviorFailure
  case class has_nav_path_to(target: String) extends NavigationBehavior:
    override def execute(actor: Mob, state: Blackboard): BehaviorStatus =
      val currentTarget = Option(actor.getNavigation.getPath).map(path => path.getTarget)
      (state.get(target), currentTarget) match {
        case (Some(posA: BlockPos), Some(posB: BlockPos))
            if posA.distManhattan(posB) <= FaeEntity.NAVIGATION_PROXIMITY =>
          BehaviorSuccess
        case _ => BehaviorFailure
      }
  case class create_nav_path_to(target: String) extends NavigationBehavior:
    override def execute(actor: Mob, state: Blackboard): BehaviorStatus =
      state.get(target) match {
        case Some(blockPos: BlockPos) =>
          actor.getNavigation.stop()
          val path = actor.getNavigation.createPath(blockPos, FaeEntity.NAVIGATION_PROXIMITY)
          if path != null && path.canReach && actor.getNavigation.moveTo(path, 1)
          then BehaviorSuccess
          else BehaviorFailure
        case _ => BehaviorFailure
      }
  case class current_path_unobstructed() extends NavigationBehavior:
    override def execute(actor: Mob, state: Blackboard): BehaviorStatus =
      if actor.getNavigation.isStuck then BehaviorFailure else BehaviorSuccess
  case class move_along_current_path() extends NavigationBehavior:
    override def execute(actor: Mob, state: Blackboard): BehaviorStatus =
      val nav = actor.getNavigation
      if nav.isInProgress then
        nav.tick()
        BehaviorRunning
      else BehaviorSuccess

sealed trait ThrottledBehavior extends Behavior[RandomTickingEntity, Blackboard]

object ThrottledBehavior:
  case class is_not_throttled() extends ThrottledBehavior:
    override def execute(actor: RandomTickingEntity, state: Blackboard): BehaviorStatus =
      if actor.isRandomTicking then BehaviorSuccess else BehaviorFailure

sealed trait FaeBehavior extends Behavior[FaeEntity, Blackboard]

object FaeBehavior:
  case class sleep() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      state.get(SPECIAL_KEYS.BED_POSITION) match {
        case Some(bedPosition: BlockPos) =>
          actor.startSleeping(bedPosition)
          // Return running here so that follow up tasks are not executed
          BehaviorRunning
        case _ => BehaviorFailure
      }

  case class is_tired() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      if actor.level().isNight then BehaviorSuccess else BehaviorFailure

  case class bed_is_valid() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      state.get(SPECIAL_KEYS.BED_POSITION) match {
        case Some(bedPosition: BlockPos) =>
          val poiManager = actor.level().asInstanceOf[ServerLevel].getPoiManager
          val blockState: BlockState = actor.level().asInstanceOf[ServerLevel].getBlockState(bedPosition)
          if blockState.is(BlockTags.BEDS)
          then BehaviorSuccess
          else BehaviorFailure
        case _ => BehaviorFailure
      }

  case class claim_bed() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      val poiManager = actor.level().asInstanceOf[ServerLevel].getPoiManager
      // TODO: instead of finding beds within a distance we should keep record of all beds within the kingdom
      val takenPOI = poiManager.take(
        holder => holder.is(PoiTypes.HOME),
        (_, bp) => {
          val blockState: BlockState = actor.level().asInstanceOf[ServerLevel].getBlockState(bp)
          blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED) == false
        },
        actor.blockPosition,
        32
      )
      if takenPOI.isPresent then
        state.update(SPECIAL_KEYS.BED_POSITION, takenPOI.get)
        BehaviorSuccess
      else BehaviorFailure

  case class is_sleeping() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      if actor.isSleeping then BehaviorSuccess else BehaviorFailure
  case class is_not_sleeping() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      if !actor.isSleeping then BehaviorSuccess else BehaviorFailure
  case class stop_sleeping() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      actor.stopSleeping()
      BehaviorSuccess
  case class target_closest_block(block: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      val predicate = blockPredicate(block)
      val maybeClosest = actor.bruteForceSearch(bp =>
        predicate(
          // TODO: Searching for blocks is really slow for some reason, probably because we get the chunk for every block
          // TODO: Searching for the nearest also means that we don't prioritize one chunk at a time, leading to cache misses
          BlockInWorld(actor.level(), bp, false)
        )
      )
      maybeClosest match {
        case Some(value) =>
          state.update("target", value)
          BehaviorSuccess
        case None => BehaviorFailure
      }
  case class target_is_block(block: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      state.get(SPECIAL_KEYS.TARGET) match {
        case Some(blockPos: BlockPos) =>
          val predicate = blockPredicate(block)
          if predicate(BlockInWorld(actor.level(), blockPos, false)) then BehaviorSuccess else BehaviorFailure
        case _ => BehaviorFailure
      }
  case class has_space_for_target_produce() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      state.get(SPECIAL_KEYS.TARGET) match {
        case Some(blockPos: BlockPos) =>
          val items = actor
            .level()
            .getBlockState(blockPos)
            .getDrops(
              LootParams
                .Builder(actor.level.asInstanceOf[ServerLevel])
                .withParameter(LootContextParams.ORIGIN, actor.position())
                .withParameter(LootContextParams.TOOL, actor.getItemInHand(InteractionHand.MAIN_HAND))
                .withParameter(LootContextParams.THIS_ENTITY, actor)
            )
            .asScala
          // TODO: See if you can pick up all items not just individuals
          if items.forall(item => actor.canTakeItem(item)) then BehaviorSuccess else BehaviorFailure
        case _ => BehaviorFailure
      }
  case class break_block(blockPos: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      state.get(SPECIAL_KEYS.TARGET) match {
        // TODO: Use player break animation/duration/tool
        case Some(blockPos: BlockPos) if actor.level().destroyBlock(blockPos, true, actor) =>
          BehaviorSuccess
        case _ =>
          BehaviorFailure
      }
  case class place_item_at_target(item: String, blockPos: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      // TODO: Investigate making item usage more generic and based on player behavior Block.use or Item.useOn
      state.get(blockPos) match {
        case Some(blockPos: BlockPos) if actor.equipFromInventory(item, EquipmentSlot.MAINHAND) =>
          val itemStack = actor.getItemInHand(InteractionHand.MAIN_HAND)
          if !itemStack.isEmpty then
            val item = itemStack.getItem
            item match {
              case blockItem: BlockItem =>
                // based on HarvestFarmland.class
                val blockState = blockItem.getBlock.defaultBlockState()
                actor.level.setBlockAndUpdate(blockPos, blockState)
                val serverLevel = actor.level.asInstanceOf[ServerLevel]
                serverLevel.gameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Context.of(actor, blockState))
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
                if itemStack.isEmpty then actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
                BehaviorSuccess
              case _ => BehaviorFailure
            }
          else BehaviorFailure
        case _ =>
          BehaviorFailure
      }
  case class holds_at_least(item: String, amount: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      if actor.getInventory.count(item) >= amount.toInt then BehaviorSuccess else BehaviorFailure
  case class holds_at_most(item: String, amount: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      if actor.getInventory.count(item) <= amount.toInt then BehaviorSuccess else BehaviorFailure
  case class target_nearest_stockpile_with(item: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      val predicate = itemPredicate(item)
      val maybeClosest =
        // TODO: Entity search instead of block search
        actor.bruteForceSearch { (bp: BlockPos) =>
          actor.level.getBlockEntity(bp, BlockEntityType.CHEST).toScala match {
            case Some(chest: ChestBlockEntity) => chest.items.count(predicate) > 0
            case _                             => false
          }
        }
      maybeClosest match {
        case Some(blockPos) =>
          state.update(SPECIAL_KEYS.TARGET, blockPos)
          BehaviorSuccess
        case None => BehaviorFailure
      }
  case class transfer_item_from_target_until(item: String, amount: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      state.get(SPECIAL_KEYS.TARGET).flatMap { case bp: BlockPos =>
        actor.level.getBlockEntity(bp, BlockEntityType.CHEST).toScala
      } match
        case Some(chest: ChestBlockEntity) =>
          val success = chest.transferItemsUntilTargetHas(actor.getInventory, item, amount.toInt)
          if success then BehaviorSuccess else BehaviorFailure
        case None => BehaviorFailure
  case class transfer_item_to_target_until(item: String, amount: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      state.get(SPECIAL_KEYS.TARGET).flatMap { case bp: BlockPos =>
        actor.level.getBlockEntity(bp, BlockEntityType.CHEST).toScala
      } match
        case Some(chest: ChestBlockEntity) =>
          val success = actor.getInventory.transferItemsUntilSelfHas(chest, item, amount.toInt)
          if success then BehaviorSuccess else BehaviorFailure
        case None => BehaviorFailure
  case class holds(item: String, min: String, max: String) extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      val count = actor.getInventory.count(item)
      if count >= min.toInt && count <= max.toInt then BehaviorSuccess else BehaviorFailure
  case class obtain_job() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      state.update("job", "farmer_wheat")
      BehaviorSuccess
  case class is_hungry() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      if actor.foodData.needsFood then BehaviorSuccess else BehaviorFailure
  case class eat_food() extends FaeBehavior:
    override def execute(actor: FaeEntity, state: Blackboard): BehaviorStatus =
      actor.getInventory.queryFirst("#c:foods") match {
        case Some(ItemSlot(itemStack, slot)) =>
          val newStack = actor.foodData.eat(itemStack)
          actor.getInventory.setItem(slot, newStack)
          BehaviorSuccess
        case None => BehaviorFailure
      }

sealed trait BlackboardBehavior extends Behavior[Any, Blackboard]:
  override def execute(actor: Any, state: Blackboard): BehaviorStatus = execute(state)
  def execute(state: Blackboard): BehaviorStatus

object BlackboardBehavior:
  case class set(key: String, value: String) extends BlackboardBehavior:
    override def execute(state: Blackboard): BehaviorStatus =
      state.get(value) match {
        case Some(value) => state.update(key, value)
        case None        => state.remove(key)
      }
      BehaviorSuccess

  case class add(key: String, value: String) extends BlackboardBehavior:
    override def execute(state: Blackboard): BehaviorStatus =
      state.get(key) match {
        case Some(acc: mutable.Set[?]) =>
          acc.asInstanceOf[mutable.Set[String]].add(value)
        case _ =>
          val t = mutable.Set(value)
          state.update(key, t)
      }
      BehaviorSuccess

  case class remove(key: String) extends BlackboardBehavior:
    override def execute(state: Blackboard): BehaviorStatus =
      state.remove(key)
      BehaviorSuccess

  case class has(value: String) extends BlackboardBehavior:
    override def execute(state: Blackboard): BehaviorStatus =
      state.get(value) match {
        case Some(_) => BehaviorSuccess
        case _       => BehaviorFailure
      }

  case class equals_literal(key: String, value: String) extends BlackboardBehavior:
    override def execute(state: Blackboard): BehaviorStatus =
      state.get(key) match {
        case Some(v) if v == value => BehaviorSuccess
        case _                     => BehaviorFailure
      }
