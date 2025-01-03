package io.github.jugbot.extension

import io.github.jugbot.extension.Vec3i.*
import net.minecraft.core.Vec3i
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB

import scala.math.addExact

object BoundingBox {
  def apply(vec: Vec3i) = new BoundingBox(vec.getX, vec.getY, vec.getZ, vec.getX, vec.getY, vec.getZ)

  extension (bb: BoundingBox) {

    /**
     * Iterates through all coordinates in the zone, ordered by increasing radial (chebyshev) distance from blockPos
     * If blockPos is inside the zone, it will be the first element
     *
     * @param blockPos    The center of the search (which will be excluded from the result)
     * @param maxDistance Distance from blockPos to the furthest coordinate to include
     */
    def closestCoordinatesInside(blockPos: Vec3i, maxDistance: Int = Int.MaxValue): Iterator[Vec3i] = {
      val startingBlock = closestCoordinateInside(blockPos)
      // TODO: Could be more efficient by skipping the generation of coordinates not in the zone
      // Longest length is from one end of the zone to the other
      val largestLength = addExact(addExact(bb.getXSpan, bb.getYSpan), bb.getZSpan)
      val startingDistance = startingBlock.chebyshevDistance(blockPos)
      val maxSearchDistance = 0.max(maxDistance - startingDistance).min(largestLength)
      for {
        distance <- Iterator.range(0, maxSearchDistance + 1)
        vec <- BoundingBox(startingBlock)
          .inflatedBy(distance)
          .sides
          .flatMap(_.intersecting(bb))
          .flatMap(_.coordinatesInside)
      } yield vec
    }

    /**
     * Returns the closest block inside the zone from the given blockPos
     */
    def closestCoordinateInside(blockPos: Vec3i) =
      Vec3i(
        blockPos.getX.max(bb.minX).min(bb.maxX),
        blockPos.getY.max(bb.minY).min(bb.maxY),
        blockPos.getZ.max(bb.minZ).min(bb.maxZ)
      )

    def coordinatesInside: Iterator[Vec3i] =
      for {
        x <- Iterator.range(bb.minX, bb.maxX + 1)
        y <- Iterator.range(bb.minY, bb.maxY + 1)
        z <- Iterator.range(bb.minZ, bb.maxZ + 1)
      } yield Vec3i(x, y, z)

    def volume: Int = bb.getXSpan * bb.getYSpan * bb.getZSpan

    private def createValidBoundingBox(minX: Int,
                                       minY: Int,
                                       minZ: Int,
                                       maxX: Int,
                                       maxY: Int,
                                       maxZ: Int
    ): Option[BoundingBox] =
      if minX <= maxX && minY <= maxY && minZ <= maxZ then Some(new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ))
      else None

    /** Corners of the bb */
    def corners: Seq[Vec3i] = Set(
      new Vec3i(bb.minX, bb.minY, bb.minZ),
      new Vec3i(bb.minX, bb.minY, bb.maxZ),
      new Vec3i(bb.minX, bb.maxY, bb.minZ),
      new Vec3i(bb.minX, bb.maxY, bb.maxZ),
      new Vec3i(bb.maxX, bb.minY, bb.minZ),
      new Vec3i(bb.maxX, bb.minY, bb.maxZ),
      new Vec3i(bb.maxX, bb.maxY, bb.minZ),
      new Vec3i(bb.maxX, bb.maxY, bb.maxZ)
    ).toSeq

    /** Edges of bb  including corners */
    def edges: Seq[BoundingBox] = corners.map(BoundingBox(_)) ++ Set(
      createValidBoundingBox(bb.minX + 1, bb.minY, bb.minZ, bb.maxX - 1, bb.minY, bb.minZ),
      createValidBoundingBox(bb.minX + 1, bb.maxY, bb.minZ, bb.maxX - 1, bb.maxY, bb.minZ),
      createValidBoundingBox(bb.minX + 1, bb.minY, bb.maxZ, bb.maxX - 1, bb.minY, bb.maxZ),
      createValidBoundingBox(bb.minX + 1, bb.maxY, bb.maxZ, bb.maxX - 1, bb.maxY, bb.maxZ),
      createValidBoundingBox(bb.minX, bb.minY + 1, bb.minZ, bb.minX, bb.maxY - 1, bb.minZ),
      createValidBoundingBox(bb.minX, bb.minY + 1, bb.maxZ, bb.minX, bb.maxY - 1, bb.maxZ),
      createValidBoundingBox(bb.maxX, bb.minY + 1, bb.minZ, bb.maxX, bb.maxY - 1, bb.minZ),
      createValidBoundingBox(bb.maxX, bb.minY + 1, bb.maxZ, bb.maxX, bb.maxY - 1, bb.maxZ),
      createValidBoundingBox(bb.minX, bb.minY, bb.minZ + 1, bb.minX, bb.minY, bb.maxZ - 1),
      createValidBoundingBox(bb.minX, bb.maxY, bb.minZ + 1, bb.minX, bb.maxY, bb.maxZ - 1),
      createValidBoundingBox(bb.maxX, bb.minY, bb.minZ + 1, bb.maxX, bb.minY, bb.maxZ - 1),
      createValidBoundingBox(bb.maxX, bb.maxY, bb.minZ + 1, bb.maxX, bb.maxY, bb.maxZ - 1)
    ).toSeq.flatten

    /** Sides of the bb not including edges and corners */
    def sides: Seq[BoundingBox] = edges ++ Set(
      createValidBoundingBox(bb.minX, bb.minY + 1, bb.minZ + 1, bb.minX, bb.maxY - 1, bb.maxZ - 1),
      createValidBoundingBox(bb.maxX, bb.minY + 1, bb.minZ + 1, bb.maxX, bb.maxY - 1, bb.maxZ - 1),
      createValidBoundingBox(bb.minX + 1, bb.minY, bb.minZ + 1, bb.maxX - 1, bb.minY, bb.maxZ - 1),
      createValidBoundingBox(bb.minX + 1, bb.maxY, bb.minZ + 1, bb.maxX - 1, bb.maxY, bb.maxZ - 1),
      createValidBoundingBox(bb.minX + 1, bb.minY + 1, bb.minZ, bb.maxX - 1, bb.maxY - 1, bb.minZ),
      createValidBoundingBox(bb.minX + 1, bb.minY + 1, bb.maxZ, bb.maxX - 1, bb.maxY - 1, bb.maxZ)
    ).toSeq.flatten

    def intersecting(other: BoundingBox): Option[BoundingBox] =
      if !bb.intersects(other) then None
      else
        val minX = bb.minX.max(other.minX)
        val minY = bb.minY.max(other.minY)
        val minZ = bb.minZ.max(other.minZ)
        val maxX = bb.maxX.min(other.maxX)
        val maxY = bb.maxY.min(other.maxY)
        val maxZ = bb.maxZ.min(other.maxZ)
        Some(new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ))

    def boundingCorners: (Vec3i, Vec3i) = (new Vec3i(bb.minX, bb.minY, bb.minZ), new Vec3i(bb.maxX, bb.maxY, bb.maxZ))

    def contains(other: BoundingBox): Boolean = {
      val corners = other.corners
      corners.forall(bb.isInside)
    }

    def toAABB: AABB = new AABB(bb.minX, bb.minY, bb.minZ, bb.maxX + 1, bb.maxY + 1, bb.maxZ + 1)
  }

  object Vec3i:
    def unapply(vec: Vec3i): (Int, Int, Int) = ((vec.getX, vec.getY, vec.getZ))
    def apply(x: Int, y: Int, z: Int): Vec3i = new Vec3i(x, y, z)
}

/**
 * Returns a triangular plane of positive coordinates that all share the same manhattan distance from the origin
 */
private def combinationsOfThreeThatSumTo(d: Int) =
  for {
    a <- Iterator.range(0, d + 1)
    b <- Iterator.range(0, d - a + 1)
    c = d - a - b
    if c >= 0
  } yield (a, b, c)
