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
  def getZonesAt(level: Level,
                 aabb: AABB,
                 variant: Option[CollisionLayer] = Option.empty[CollisionLayer]
  ): Array[ZoneEntity] =
    level
      .getEntities(
        EntityTypeTest.forClass(classOf[ZoneEntity]),
        aabb,
        (e: Entity) =>
          e match {
            case zoneEntity: ZoneEntity => variant.isEmpty || zoneEntity.getCollisionLayer.eq(variant.get)
            case _                      => false
          }
      )
      .asScala
      .toArray

  def canFitAt(level: Level, bb: AABB, collisionLayer: CollisionLayer): Boolean = {
    val aabb = bb.deflate(0.1) // Superstitiously avoid floating point errors
    val isNotColliding = getZonesAt(level, aabb, Option(collisionLayer)).isEmpty
    val parentLayer = collisionLayer.parent
    val isContainedProperly = parentLayer match {
      case Some(layer) =>
        getZonesAt(level, aabb, Option(layer))
          .exists((e: ZoneEntity) => e.getBoundingBox.contains(aabb))
      case None => true
    }

    isNotColliding && isContainedProperly
  }
}
