package io.github.jugbot.extension

import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.{AABB as MinecraftAABB, Vec3}
import net.minecraft.core.BlockPos

object AABB {
  extension (aabb: MinecraftAABB)
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

    def contains(other: MinecraftAABB): Boolean = other.corners.forall(aabb.contains)

    def toBoundingBox: BoundingBox = BoundingBox(
      aabb.minX.round.toInt,
      aabb.minY.round.toInt,
      aabb.minZ.round.toInt,
      aabb.maxX.round.toInt - 1,
      aabb.maxY.round.toInt - 1,
      aabb.maxZ.round.toInt - 1
    )

  def withRadius(center: BlockPos, radius: Int): MinecraftAABB =
    withRadius(center.getCenter(), radius)

  def withRadius(center: Vec3, radius: Int): MinecraftAABB =
    val size = radius * 2 + 1
    MinecraftAABB.ofSize(center, size, size, size)
}
