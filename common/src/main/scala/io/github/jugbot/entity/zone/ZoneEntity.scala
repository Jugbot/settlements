package io.github.jugbot.entity.zone

import io.github.jugbot.extension.AABB.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.{Entity, EntityType, LightningBolt}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.{Mirror, Rotation}
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.AABB
import net.minecraft.world.{InteractionHand, InteractionResult}

enum CollisionLayer:
  case Settlement, Zoning, Structure
  private val layers = List(Settlement, Zoning, Structure)
  def parent: Option[CollisionLayer] = layers.lift(layers.indexOf(this) - 1)
  def child: Option[CollisionLayer] = layers.lift(layers.indexOf(this) + 1)

abstract class ZoneEntity(entityType: EntityType[? <: ZoneEntity], world: Level) extends Entity(entityType, world) {

  def this(world: Level, boundingBox: AABB) = {
    this(null.asInstanceOf[EntityType[? <: ZoneEntity]], world)
    setBoundingBox(boundingBox)
    resetPositionToBB()
  }

  def getCollisionLayer: CollisionLayer

  private def resetPositionToBB(): Unit = {
    val bb = getBoundingBox
    setPosRaw(bb.getCenter.x, bb.getCenter.y, bb.getCenter.z);
  }

  override def setPos(d: Double, e: Double, f: Double): Unit =
    super.setPos(d.round.toInt, e.round.toInt, f.round.toInt)

  override def moveTo(x: Double, y: Double, z: Double, yRot: Float, xRot: Float): Unit =
    super.moveTo(x.round.toInt, y.round.toInt, z.round.toInt, 0, 0)

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit = {}

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {}

  override def defineSynchedData(): Unit = {}

  override def canAddPassenger(entity: Entity): Boolean = false

  override def canCollideWith(entity: Entity): Boolean = false

  override def hurt(damageSource: DamageSource, f: Float): Boolean = false

  override def tick(): Unit = {
    xRotO = getXRot
    yRotO = getYRot
    walkDistO = walkDist
    xo = getX
    yo = getY
    zo = getZ

    if getBoundingBox.getXsize == 0 then discard()
  }

  override def interact(player: Player, interactionHand: InteractionHand): InteractionResult = InteractionResult.PASS

  override def repositionEntityAfterLoad(): Boolean = false

  override def rotate(transformRotation: Rotation): Float = super.rotate(transformRotation)

  override def mirror(mirror: Mirror): Float = super.mirror(mirror)

  override def thunderHit(serverLevel: ServerLevel, lightningBolt: LightningBolt): Unit = {}

  override def refreshDimensions(): Unit = {}

  override def getAddEntityPacket: Packet[ClientGamePacketListener] = super.getAddEntityPacket

  override def isIgnoringBlockTriggers: Boolean = true

  override def getPistonPushReaction: PushReaction = PushReaction.IGNORE

  override def canChangeDimensions: Boolean = false
}
