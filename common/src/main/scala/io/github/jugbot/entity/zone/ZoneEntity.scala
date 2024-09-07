package io.github.jugbot.entity.zone

import dev.architectury.extensions.network.EntitySpawnExtension
import dev.architectury.networking.NetworkManager
import io.github.jugbot.Mod
import io.github.jugbot.extension.AABB.*
import io.github.jugbot.extension.CompoundTag.{getEntities, putEntities}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.{ClientGamePacketListener, ClientboundAddEntityPacket}
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity.RemovalReason
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.{Entity, EntityType, LightningBolt}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.{Mirror, Rotation}
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.AABB
import net.minecraft.world.{InteractionHand, InteractionResult}

import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala

abstract class ZoneEntity(entityType: EntityType[? <: ZoneEntity], world: Level)
    extends Entity(entityType, world)
    with EntitySpawnExtension {

  private var parentZone: Option[ZoneEntity] = None

  def getParentZone: Option[ZoneEntity] = parentZone

  private var childZones: mutable.Set[ZoneEntity] = mutable.Set.empty

  def getChildZones: Set[ZoneEntity] = Set.empty ++ childZones

  private def linkChild(childZone: ZoneEntity): Unit = {
    childZones.add(childZone)
    childZone.parentZone = Some(this)
  }

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

    childZones = mutable.Set.empty ++= (
      compoundTag.getEntities(
        "children",
        uuid =>
          level match {
            case level: ServerLevel => Some(level.getEntity(uuid))
            case level =>
              level
                .getEntitiesOfClass[ZoneEntity](classOf[ZoneEntity],
                                                getBoundingBox,
                                                (e: ZoneEntity) => e.getUUID == uuid
                )
                .asScala
                .headOption
          }
      )
    )
    childZones.foreach(_.parentZone = Some(this))
  }

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    val bb = getBoundingBox
    compoundTag.putDouble("minX", bb.minX)
    compoundTag.putDouble("minY", bb.minY)
    compoundTag.putDouble("minZ", bb.minZ)
    compoundTag.putDouble("maxX", bb.maxX)
    compoundTag.putDouble("maxY", bb.maxY)
    compoundTag.putDouble("maxZ", bb.maxZ)

    compoundTag.putEntities("children", getChildZones)
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

    if parentZone.exists(_.isRemoved) then remove(RemovalReason.DISCARDED)
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

  override def remove(removalReason: RemovalReason): Unit = super.remove(removalReason)

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

object ZoneEntity {
  private def validate(zone: ZoneEntity, parentZone: Option[ZoneEntity] = None): Either[String, Unit] =
    if zone.getZoneType.validParents.nonEmpty && !parentZone.exists(z =>
        zone.getZoneType.validParents.apply(z.getZoneType)
      )
    then Left("Attempted to create a zone without its parent")
    else if parentZone.isDefined && !parentZone.get.getBoundingBox.contains(zone.getBoundingBox) then
      Left("Attempted to create a child zone outside of parent zone")
    else if ZoneManager.getConflicting(zone.level, zone.getBoundingBox, zone.getZoneType).nonEmpty then
      Left("Attempted to create a zone in a conflicting area")
    else Right(())

  def makeZone(entityType: EntityType[? <: ZoneEntity], level: Level, aabb: AABB) = {
    val zone = entityType.create(level)
    zone.updateBounds(aabb)
    validate(zone) match {
      case Left(msg) =>
        Mod.LOGGER.error(msg)
        zone.discard()
        None
      case Right(_) =>
        level.addFreshEntity(zone)
        Some(zone)
    }
  }

  def makeChildZone(parent: ZoneEntity, entityType: EntityType[? <: ZoneEntity], aabb: AABB): Option[ZoneEntity] = {
    val zone = entityType.create(parent.level)
    zone.updateBounds(aabb)
    validate(zone, Some(parent)) match {
      case Left(msg) =>
        Mod.LOGGER.error(msg)
        zone.discard()
        None
      case Right(_) =>
        parent.linkChild(zone)
        parent.level.addFreshEntity(zone)
        Some(zone)
    }
  }
}
