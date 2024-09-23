package io.github.jugbot.entity.zone

import com.google.common.base.Suppliers
import io.github.jugbot.blockentity.ShrineBlockEntity
import io.github.jugbot.entity.{FaeEntity, Owning}
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.{Entity, EntityType, MobCategory, MobSpawnType}
import net.minecraft.world.level.Level

import java.util.UUID
import java.util.function.Supplier
import scala.jdk.CollectionConverters.CollectionHasAsScala

class SettlementZoneEntity(entityType: EntityType[SettlementZoneEntity], world: Level)
    extends ZoneEntity(entityType, world)
    with Owning[SettlementZoneEntity, FaeEntity]("Settlers") {

  override def getZoneType: ZoneType = ZoneType.Settlement

  def settlers: Set[FaeEntity] = children
  val addSettler = addChild _

  def getShrineZone: Option[ShrineZoneEntity] =
    getChildZones.find(_.isInstanceOf[ShrineZoneEntity]).map(_.asInstanceOf[ShrineZoneEntity])

  def getShrine: Option[ShrineBlockEntity] = getShrineZone.flatMap(_.getShrine)

  def getMaxSettlers: Int =
    getShrine match {
      case Some(shrine) =>
        shrine.tier match {
          case 0 => 1
          case 1 => 2
          case 2 => 15
          case 3 => 20
          case _ => 0
        }
      case None => 0
    }

  private def getSpawnPosition: Option[BlockPos] = getShrine.map(_.getBlockPos)

  private def spawnSettler(): Unit =
    (this.level, getSpawnPosition) match {
      case (serverLevel: ServerLevel, Some(spawnPos)) =>
        addSettler(FaeEntity.TYPE.get.spawn(serverLevel, spawnPos, MobSpawnType.MOB_SUMMONED))
      case _ =>
    }

  // TODO: Soft despawn to avoid dropping items
  private def despawnSettler(): Unit =
    if settlers.nonEmpty then settlers.last.kill()

  private def getEntity(uuid: UUID): Option[FaeEntity] = level match {
    case serverLevel: ServerLevel => Option(serverLevel.getEntity(uuid)).map(_.asInstanceOf[FaeEntity])
    case clientLevel: ClientLevel =>
      clientLevel
        .getEntitiesOfClass(classOf[FaeEntity], getBoundingBox, (e: FaeEntity) => e.getUUID == uuid)
        .asScala
        .headOption
    case _ => None
  }

  private def removeStaleSettlers(): Unit =
    children = settlers.filter(_.isAlive)

  override def tick(): Unit =
    super.tick()
    if settlers.size < getMaxSettlers then spawnSettler()
    else if settlers.size > getMaxSettlers then despawnSettler()
    // TODO: Fix tick before passenger can be added
    if !getShrineZone.exists(_.isAlive) && !level.isClientSide then discard()
    removeStaleSettlers()

  override def remove(removalReason: Entity.RemovalReason): Unit =
    super.remove(removalReason)
    settlers.foreach(_.kill)

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit =
    super.addAdditionalSaveData(compoundTag)

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit =
    super.readAdditionalSaveData(compoundTag)
}

object SettlementZoneEntity {
  val DEFAULT_RADIUS = 12

  final val TYPE: Supplier[EntityType[SettlementZoneEntity]] = Suppliers.memoize(() =>
    EntityType.Builder
      .of[SettlementZoneEntity](new SettlementZoneEntity(_, _), MobCategory.MISC)
      .sized(0, 0)
      .build("settlement_zone")
  )
}
