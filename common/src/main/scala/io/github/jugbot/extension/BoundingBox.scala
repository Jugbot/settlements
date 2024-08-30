package io.github.jugbot.extension

import io.github.jugbot.extension.LazyRange.*
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth.clamp
import net.minecraft.world.level.levelgen.structure.BoundingBox

import scala.math.addExact

object BoundingBox {
  extension (bb: BoundingBox) {

    /**
     * Iterates through all coordinates in the zone, ordered by increasing distance from blockPos
     *
     * @param blockPos    The center of the search (which will be excluded from the result)
     * @param maxDistance Distance from blockPos to the furthest coordinate to include
     */
    def closestCoordinatesInside(blockPos: Vec3i, maxDistance: Int = Int.MaxValue): LazyList[Vec3i] = {
      val startingBlock = closestCoordinateInside(blockPos)
      // TODO: Could be more efficient by skipping the generation of coordinates not in the zone
      // Longest length is from one end of the zone to the other
      val largestLength = addExact(addExact(bb.getXSpan, bb.getYSpan), bb.getZSpan)
      val maxSearchDistance = clamp(0, maxDistance - blockPos.distManhattan(startingBlock), largestLength)
      for {
        distance <- 0.toLazy(maxSearchDistance)
        // Get the planar combinations of coordinates that sum to distance
        xSign <- Seq(-1, 1)
        ySign <- Seq(-1, 1)
        zSign <- Seq(-1, 1)
        (x, y, z) <- combinationsOfThreeThatSumTo(distance)
        // Avoid mirroring if the coordinate if the coordinate wont change (i.e. zeroes)
        if xSign != x + 1 && ySign != y + 1 && zSign != z + 1
        result = startingBlock.offset(x * xSign, y * ySign, z * zSign)
        if bb.isInside(result)
        if result != blockPos
      } yield result
    }

    /**
     * Returns the closest block inside the zone from the given blockPos
     */
    def closestCoordinateInside(blockPos: Vec3i) =
      Vec3i(
        clamp(blockPos.getX, bb.minX, bb.maxX),
        clamp(blockPos.getY, bb.minY, bb.maxY),
        clamp(blockPos.getZ, bb.minZ, bb.maxZ)
      )

    def volume: Int = bb.getXSpan * bb.getYSpan * bb.getZSpan
  }
}

/**
 * Returns a triangular plane of positive coordinates that all share the same manhattan distance from the origin
 */
private def combinationsOfThreeThatSumTo(d: Int) =
  for {
    a <- 0.toLazy(d)
    b <- 0.toLazy(d - a)
    c = d - a - b
    if c >= 0
  } yield (a, b, c)
