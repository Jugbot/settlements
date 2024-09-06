package io.github.jugbot.entity.zone

import io.github.jugbot.extension.AABB.*
import net.minecraft.core.BlockPos
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

  def canFitAt(level: Level, aabb: AABB, collisionLayer: ZoneType): Boolean = {
    val collidingZones = getZonesAt(level, aabb, Option(collisionLayer))
    val isNotColliding = collidingZones.isEmpty
    val parentLayers = collisionLayer.validParents
    val isContainedProperly = parentLayers.isEmpty || parentLayers.exists(layer =>
      getZonesAt(level, aabb, Option(layer))
        .exists((e: ZoneEntity) => e.getBoundingBox.contains(aabb))
    )

    isNotColliding && isContainedProperly
  }

  def getConflicting(level: Level, aabb: AABB, collisionLayer: ZoneType): Array[ZoneEntity] =
    getZonesAt(level, aabb, Option(collisionLayer))

  def spawnWithAABB[Z <: ZoneEntity](level: Level, supplier: Function[Level, Z], aabb: AABB): Z = {
    val zoneEntity = supplier(level)
    zoneEntity.updateBounds(aabb)
    level.addFreshEntity(zoneEntity)
    zoneEntity
  }

  def aabbWithRadius(center: BlockPos, radius: Int): AABB =
    val size = radius * 2 + 1
    AABB.ofSize(center.getCenter, size, size, size)

  def spawnWithRadius[Z <: ZoneEntity](level: Level, supplier: Function[Level, Z], center: BlockPos, radius: Int): Z = {
    val aabb = aabbWithRadius(center, radius)
    spawnWithAABB(level, supplier, aabb)
  }
}
