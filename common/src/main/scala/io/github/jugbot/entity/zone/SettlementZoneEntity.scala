package io.github.jugbot.entity.zone

import com.google.common.base.Suppliers
import io.github.jugbot.blockentity.ShrineBlockEntity
import io.github.jugbot.entity.FaeEntity
import io.github.jugbot.extension.AABB.*
import io.github.jugbot.extension.CompoundTag.{getEntities, putEntities}
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.{Entity, EntityType, MobCategory, MobSpawnType}
import net.minecraft.world.level.Level

import java.util.UUID
import java.util.function.Supplier
import scala.collection.mutable

class SettlementZoneEntity(entityType: EntityType[SettlementZoneEntity], world: Level)
    extends ZoneEntity(entityType, world) {
  override def getZoneType: ZoneType = ZoneType.Settlement

  val settlers = mutable.Stack.empty[FaeEntity]

  def getShrineZone: Option[ShrineZoneEntity] =
    getChildZones.find(_.isInstanceOf[ShrineZoneEntity]).map(_.asInstanceOf[ShrineZoneEntity])

  def getShrine: Option[ShrineBlockEntity] = getShrineZone.flatMap(_.getShrine)

  def getMaxSettlers: Int =
    getShrine match {
      case Some(shrine) =>
        shrine.tier match {
          case 0 => 5
          case 1 => 10
          case 2 => 15
          case 3 => 20
          case _ => 0
        }
      case None => 0
    }

  def isOwnedBy(playerUUID: UUID): Boolean = getShrine.exists(_.owners.contains(playerUUID))

  private def getSpawnPosition: Option[BlockPos] = getShrine.map(_.getBlockPos)

  private def spawnSettler(): Unit =
    (this.level, getSpawnPosition) match {
      case (serverLevel: ServerLevel, Some(spawnPos)) =>
        settlers.push(FaeEntity.TYPE.get.spawn(serverLevel, spawnPos, MobSpawnType.MOB_SUMMONED))
      case _ =>
    }

  // TODO: Soft despawn to avoid dropping items
  private def despawnSettler(): Unit =
    if settlers.nonEmpty then settlers.pop().kill()

  override def tick(): Unit =
    super.tick()
    if settlers.size < getMaxSettlers then spawnSettler()
    else if settlers.size > getMaxSettlers then despawnSettler()

  override def remove(removalReason: Entity.RemovalReason): Unit =
    super.remove(removalReason)
    settlers.foreach(_.kill())

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit =
    super.addAdditionalSaveData(compoundTag)
    compoundTag.putEntities("settlers", settlers)

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit =
    super.readAdditionalSaveData(compoundTag)
    settlers.clear()
    compoundTag
      .getEntities[FaeEntity]("settlers",
                              uuid =>
                                level match {
                                  // TODO: Won't be synced client side?
                                  case serverLevel: ServerLevel => Some(serverLevel.getEntity(uuid))
                                  case _                        => None
                                }
      )
      .foreach(settlers.push)
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
