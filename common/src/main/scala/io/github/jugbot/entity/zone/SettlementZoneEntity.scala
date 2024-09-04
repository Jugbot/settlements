package io.github.jugbot.entity.zone

import com.google.common.base.Suppliers
import io.github.jugbot.blockentity.ShrineBlockEntity
import io.github.jugbot.extension.AABB.*
import net.minecraft.world.entity.{EntityType, MobCategory}
import net.minecraft.world.level.Level

import java.util.UUID
import java.util.function.Supplier

class SettlementZoneEntity(entityType: EntityType[SettlementZoneEntity], world: Level)
    extends ZoneEntity(entityType, world) {
  override def getZoneType: ZoneType = ZoneType.Settlement

  private var maybeShrine: Option[ShrineBlockEntity] = Option.empty

  def getShrine: Option[ShrineBlockEntity] = maybeShrine

  def hasShrine: Boolean = maybeShrine.isDefined

  def hasOwner: Boolean = maybeShrine.contains((e: ShrineBlockEntity) => e.owners.nonEmpty)

  def isOwnedBy(playerUUID: UUID): Boolean = maybeShrine.exists(_.owners.contains(playerUUID))

  override def tick(): Unit =
    super.tick()
    level.getBlockEntity(this.getBoundingBox.toBoundingBox.getCenter) match {
      case shrineBlockEntity: ShrineBlockEntity => maybeShrine = Option(shrineBlockEntity)
      case _                                    => maybeShrine = Option.empty
    }

}

object SettlementZoneEntity {
  val DEFAULT_RADIUS = 12

  final val TYPE: Supplier[EntityType[SettlementZoneEntity]] = Suppliers.memoize(() =>
    EntityType.Builder
      .of[SettlementZoneEntity](new SettlementZoneEntity(_, _), MobCategory.MISC)
      .build("settlement_zone")
  )
}
