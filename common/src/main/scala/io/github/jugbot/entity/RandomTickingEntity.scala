package io.github.jugbot.entity

import dev.architectury.event.events.common.TickEvent
import net.minecraft.world.entity.Entity

import scala.collection.mutable
import scala.util.Random

trait RandomTickingEntity extends Entity {
  var isRandomTicking: Boolean = false

  RandomTickingEntity.REGISTRY += this;

  override def remove(removalReason: Entity.RemovalReason): Unit =
    super.remove(removalReason)
    RandomTickingEntity.REGISTRY -= this

  override def tick(): Unit = {
    this.isRandomTicking = RandomTickingEntity.isRandomTicking(this)
    if this.isRandomTicking then {
      this.randomTick()
    }
    super.tick()
  }

  def randomTick(): Unit = {}
}

private object RandomTickingEntity {
  private val REGISTRY = mutable.LinkedHashSet[Entity]();
  private var SELECTED_ENTITIES = Set[Entity]();

  // TODO: Make this configurable
  private val MAX_ENTITY_SELECTION = 1;

  private def isRandomTicking(entity: Entity): Boolean =
    SELECTED_ENTITIES.contains(entity)

  private def tick(): Unit = {
    val random = new Random()
    SELECTED_ENTITIES = random.shuffle(REGISTRY).take(MAX_ENTITY_SELECTION).toSet
  }

  TickEvent.SERVER_PRE.register(_ => RandomTickingEntity.tick())
}
