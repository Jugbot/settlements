package io.github.jugbot.entity

import com.google.common.base.Suppliers
import io.github.jugbot.ai.*
import io.github.jugbot.ai.tree.{Behavior, Blackboard, FaeBehaviorTree}
import io.github.jugbot.entity.zone.SettlementZoneEntity
import io.github.jugbot.extension.AABB.*
import io.github.jugbot.extension.BoundingBox.*
import io.github.jugbot.meta
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.{AttributeSupplier, Attributes}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.{InteractionHand, InteractionResult}

import java.util.function.Supplier
import scala.collection.mutable

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

  private val blackboard = mutable.Map.empty[String, Any]

  private def performBehavior(name: String, args: Map[String, String]): BehaviorStatus =
    val profiler = this.level().getProfiler
    val behavior = meta.valueOf[Behavior[FaeEntity, Blackboard]](name, args).get
    profiler.push(behavior.toString)
    val result = behavior.execute(this, blackboard)
    profiler.pop()
    result

  override def mobInteract(player: Player, interactionHand: InteractionHand): InteractionResult =
    if player.isCrouching && !player.isLocalPlayer then {
      player.sendSystemMessage(Component.literal("Behavior Log:").withStyle(ChatFormatting.BOLD))
      behaviorLog.foreach { log =>
        val basicMessage = Component.literal(log.toString())
        val styledMessage =
          if log.isModule then basicMessage.withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD)
          else basicMessage.withStyle(ChatFormatting.BLUE)
        player.sendSystemMessage(styledMessage)
      }
      InteractionResult.PASS
    } else {
      super.mobInteract(player, interactionHand)
    }

  def settlementZone: Option[SettlementZoneEntity] = parent

}

object FaeEntity {
  val NAVIGATION_PROXIMITY = 1
  val FOLLOW_RANGE = 20f

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
      .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)

}
