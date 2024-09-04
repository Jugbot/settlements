package io.github.jugbot.entity.zone

import dev.architectury.extensions.network.EntitySpawnExtension
import dev.architectury.networking.NetworkManager
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.{ClientGamePacketListener, ClientboundAddEntityPacket}
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.{Entity, EntityType, LightningBolt}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.{Mirror, Rotation}
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.AABB
import net.minecraft.world.{InteractionHand, InteractionResult}

abstract class ZoneEntity(entityType: EntityType[? <: ZoneEntity], world: Level)
    extends Entity(entityType, world)
    with EntitySpawnExtension {
  def getZoneType: ZoneType

  def updateBounds(aabb: AABB): Unit = {
    setBoundingBox(aabb)
    setPosRaw(aabb.getCenter.x, aabb.getCenter.y, aabb.getCenter.z);
  }

  override def setPos(x: Double, y: Double, z: Double): Unit =
    updateBounds(getBoundingBox.move(x - getX, y - getY, z - getZ))

  override def moveTo(x: Double, y: Double, z: Double, yRot: Float, xRot: Float): Unit =
    super.moveTo(x, y, z, 0, 0)

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    val minX = compoundTag.getDouble("minX")
    val minY = compoundTag.getDouble("minY")
    val minZ = compoundTag.getDouble("minZ")
    val maxX = compoundTag.getDouble("maxX")
    val maxY = compoundTag.getDouble("maxY")
    val maxZ = compoundTag.getDouble("maxZ")
    updateBounds(AABB(minX, minY, minZ, maxX, maxY, maxZ))
  }

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    val bb = getBoundingBox
    compoundTag.putDouble("minX", bb.minX)
    compoundTag.putDouble("minY", bb.minY)
    compoundTag.putDouble("minZ", bb.minZ)
    compoundTag.putDouble("maxX", bb.maxX)
    compoundTag.putDouble("maxY", bb.maxY)
    compoundTag.putDouble("maxZ", bb.maxZ)
  }

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
  }

  override def interact(player: Player, interactionHand: InteractionHand): InteractionResult = InteractionResult.PASS

  override def repositionEntityAfterLoad(): Boolean = false

  override def rotate(transformRotation: Rotation): Float = super.rotate(transformRotation)

  override def mirror(mirror: Mirror): Float = super.mirror(mirror)

  override def thunderHit(serverLevel: ServerLevel, lightningBolt: LightningBolt): Unit = {}

  override def refreshDimensions(): Unit = {}

  override def isIgnoringBlockTriggers: Boolean = true

  override def getPistonPushReaction: PushReaction = PushReaction.IGNORE

  override def canChangeDimensions: Boolean = false

  /**
   * In order to resolve the issue of the game client not knowing the dynamic bounding box size set during initialization, we can sneak that data in place of the delta movement.
   */
  override def getAddEntityPacket: Packet[ClientGamePacketListener] = NetworkManager.createAddEntityPacket(this)
  override def recreateFromPacket(clientboundAddEntityPacket: ClientboundAddEntityPacket): Unit =
    super.recreateFromPacket(clientboundAddEntityPacket)

  def saveAdditionalSpawnData(buf: FriendlyByteBuf): Unit = {
    buf.writeDouble(getBoundingBox.minX)
    buf.writeDouble(getBoundingBox.minY)
    buf.writeDouble(getBoundingBox.minZ)
    buf.writeDouble(getBoundingBox.maxX)
    buf.writeDouble(getBoundingBox.maxY)
    buf.writeDouble(getBoundingBox.maxZ)
  }

  def loadAdditionalSpawnData(buf: FriendlyByteBuf): Unit = {
    val minX = buf.readDouble()
    val minY = buf.readDouble()
    val minZ = buf.readDouble()
    val maxX = buf.readDouble()
    val maxY = buf.readDouble()
    val maxZ = buf.readDouble()
    updateBounds(AABB(minX, minY, minZ, maxX, maxY, maxZ))
  }
}
