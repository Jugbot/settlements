package io.github.jugbot.entity.zone

import com.google.common.base.Suppliers
import net.minecraft.world.entity.{EntityType, MobCategory}
import net.minecraft.world.level.Level

import java.util.function.Supplier

class ShrineZoneEntity(entityType: EntityType[ShrineZoneEntity], world: Level) extends ZoneEntity(entityType, world) {
  override def getCollisionLayer: CollisionLayer = CollisionLayer.Structure
}

object ShrineZoneEntity {
  val DEFAULT_RADIUS = 7

  final val TYPE: Supplier[EntityType[ShrineZoneEntity]] = Suppliers.memoize(() =>
    EntityType.Builder
      .of[ShrineZoneEntity](new ShrineZoneEntity(_, _), MobCategory.MISC)
      .sized(DEFAULT_RADIUS + 0.5f, DEFAULT_RADIUS + 0.5f)
      .build("shrine_zone")
  )
}
