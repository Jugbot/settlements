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
import io.github.jugbot.meta
import io.github.jugbot.ai.tree.Behavior
import io.github.jugbot.ai.tree.Blackboard

class FaeEntity(entityType: EntityType[FaeEntity], world: Level)
    extends Mob(entityType, world)
    with ExtraInventory
    with RandomTickingEntity
    with Hunger
    with WithParent[FaeEntity, SettlementZoneEntity] {

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
      this.performBehavior,
      FaeBehaviorTree.map
    )
    ()
  }

  private object SPECIAL_KEYS {
    val TARGET = "target"
    val BED_POSITION = "bed_position"
  }

  private val blackboard = mutable.Map.empty[String, Any]

  private def performBehavior(name: String, args: Map[String, String]): BehaviorStatus =
    val profiler = this.level().getProfiler
    val behavior = meta.valueOf[Behavior[FaeEntity, Blackboard]](name, args).get
    profiler.push(behavior.toString)
    val result = behavior.execute(this, blackboard)
    profiler.pop()
    result

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

  private def settlementZone = parent

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
  def bruteForceSearch(predicate: Function[BlockPos, Boolean]): Option[BlockPos] =
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
  val BED_POSITION_NBT_KEY = "bedPosition"

  val NAVIGATION_PROXIMITY = 1
  val BRUTE_FORCE_SEARCH_RADIUS = 12
  val BRUTE_FORCE_SEARCH_ATTEMPTS = 20

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
