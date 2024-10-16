package io.github.jugbot.entity.zone

import io.github.jugbot.extension.AABB.*
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB

import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * Manages zones and enforces constraints such as tree hierarchy and collision rules.
 */
object ZoneManager {

  // TODO: Efficient 3D range lookup?
  def getZonesAt(level: Level, bb: AABB, variant: Option[ZoneType] = Option.empty[ZoneType]): Array[ZoneEntity] =
    val veryLargeAABB = bb.inflate(16 * 6) // HACK: The getEntities method is not precise with aabb lookup
    val aabb = bb.deflate(0.1) // Superstitiously avoid floating point errors
    level
      .getEntities(
        EntityTypeTest.forClass(classOf[ZoneEntity]),
        veryLargeAABB,
        (e: Entity) =>
          e match {
            case zoneEntity: ZoneEntity if aabb.intersects(zoneEntity.getBoundingBox) =>
              variant.isEmpty || zoneEntity.getZoneType == variant.get
            case _ => false
          }
      )
      .asScala
      .toArray

  /** Returns overlapping siblings */
  def getConflicting(level: Level, aabb: AABB, zone: ZoneEntity): Array[ZoneEntity] =
    getConflicting(level, aabb, zone.getZoneType).filterNot(_ == zone)

  def getConflicting(level: Level, aabb: AABB, collisionLayer: ZoneType): Array[ZoneEntity] =
    getZonesAt(level, aabb, Option(collisionLayer))
}
