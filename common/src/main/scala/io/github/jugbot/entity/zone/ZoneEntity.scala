package io.github.jugbot.entity.zone

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.{Entity, EntityType, LightningBolt}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.{Mirror, Rotation}
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.AABB
import net.minecraft.world.{InteractionHand, InteractionResult}

class ZoneEntity(entityType: EntityType[ZoneEntity], world: Level, private var bb: BoundingBox)
    extends Entity(entityType, world) {
  setBoundingBox(AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ))

  def getBB: BoundingBox = bb

  def setBB(bb: BoundingBox): Unit = {
    this.bb = bb
    setBoundingBox(AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ))
  }

  override def defineSynchedData(): Unit = {}

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit = {}

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {}

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
