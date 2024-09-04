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
  def getZonesAt(level: Level, aabb: AABB, variant: Option[ZoneType] = Option.empty[ZoneType]): Array[ZoneEntity] =
    level
      .getEntities(
        EntityTypeTest.forClass(classOf[ZoneEntity]),
        aabb,
        (e: Entity) =>
          e match {
            case zoneEntity: ZoneEntity => variant.isEmpty || zoneEntity.getZoneType.eq(variant.get)
            case _                      => false
          }
      )
      .asScala
      .toArray

  def canFitAt(level: Level, bb: AABB, collisionLayer: ZoneType): Boolean = {
    val aabb = bb.deflate(0.1) // Superstitiously avoid floating point errors
    val isNotColliding = getZonesAt(level, aabb, Option(collisionLayer)).isEmpty
    val parentLayers = collisionLayer.validParents
    val isContainedProperly = parentLayers.isEmpty || parentLayers.exists(layer =>
      getZonesAt(level, aabb, Option(layer))
        .exists((e: ZoneEntity) => e.getBoundingBox.contains(aabb))
    )

    isNotColliding && isContainedProperly
  }

  def spawnWithAABB[Z <: ZoneEntity](level: Level, supplier: Function[Level, Z], aabb: AABB): Z = {
    val zoneEntity = supplier(level)
    zoneEntity.updateBounds(aabb)
    level.addFreshEntity(zoneEntity)
    zoneEntity
  }

  def spawnWithRadius[Z <: ZoneEntity](level: Level, supplier: Function[Level, Z], center: BlockPos, radius: Int): Z = {
    val size = radius * 2 + 1
    val aabb = AABB.ofSize(center.getCenter, size, size, size)
    spawnWithAABB(level, supplier, aabb)
  }
}
