package io.github.jugbot.extension

import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.{AABB, Vec3}

object AABB {
  extension (aabb: net.minecraft.world.phys.AABB)
    def corners: Seq[Vec3] = Seq(
      Vec3(aabb.minX, aabb.minY, aabb.minZ),
      Vec3(aabb.minX, aabb.minY, aabb.maxZ),
      Vec3(aabb.minX, aabb.maxY, aabb.minZ),
      Vec3(aabb.minX, aabb.maxY, aabb.maxZ),
      Vec3(aabb.maxX, aabb.minY, aabb.minZ),
      Vec3(aabb.maxX, aabb.minY, aabb.maxZ),
      Vec3(aabb.maxX, aabb.maxY, aabb.minZ),
      Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)
    )

    def contains(other: AABB): Boolean = other.corners.forall(aabb.contains)

    def toBoundingBox: BoundingBox = BoundingBox(
      aabb.minX.round.toInt,
      aabb.minY.round.toInt,
      aabb.minZ.round.toInt,
      aabb.maxX.round.toInt - 1,
      aabb.maxY.round.toInt - 1,
      aabb.maxZ.round.toInt - 1
    )
}
