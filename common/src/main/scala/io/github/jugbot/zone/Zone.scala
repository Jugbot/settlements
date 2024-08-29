package io.github.jugbot.zone

import net.minecraft.core.{BlockPos, Vec3i}
import net.minecraft.util.Mth.clamp
import net.minecraft.world.level.levelgen.structure.BoundingBox

class Zone(xA: Int, yA: Int, zA: Int, xB: Int, yB: Int, zB: Int) extends BoundingBox(xA, yA, zA, xB, yB, zB) {

  def this(blockPos: BlockPos) =
    this(blockPos.getX, blockPos.getY, blockPos.getZ, blockPos.getX + 1, blockPos.getY + 1, blockPos.getZ + 1)

  def this(blockPosA: BlockPos, blockPosB: BlockPos) =
    this(blockPosA.getX, blockPosA.getY, blockPosA.getZ, blockPosB.getX, blockPosB.getY, blockPosB.getZ)

  def this(vec3A: Vec3i, vec3B: Vec3i) =
    this(vec3A.getX, vec3A.getY, vec3A.getZ, vec3B.getX, vec3B.getY, vec3B.getZ)

  /**
   * Iterates through all coordinates in the zone, ordered by increasing distance from blockPos
   * @param blockPos
   */
  def closestCoordinatesInside(blockPos: Vec3i, maxDistance: Int = Int.MaxValue): IndexedSeq[Vec3i] = {
    val startingBlock = closestCoordinateInside(blockPos)
    // TODO: Could be more efficient by skipping the generation of coordinates not in the zone
    // Longest length is from one end of the zone to the other
    val largestLength = this.getXSpan + this.getYSpan + this.getZSpan
    val maxSearchDistance = clamp(0, maxDistance - startingBlock.distManhattan(blockPos), largestLength)
    for {
      distance <- 0 to maxSearchDistance
      // Get the planar combinations of coordinates that sum to distance
      xSign <- Seq(-1, 1)
      ySign <- Seq(-1, 1)
      zSign <- Seq(-1, 1)
      (x, y, z) <- combinationsOfThreeThatSumTo(distance)
      // Avoid mirroring if the coordinate if the coordinate wont change (i.e. zeroes)
      if xSign != x + 1 && ySign != y + 1 && zSign != z + 1
      result = startingBlock.offset(x * xSign, y * ySign, z * zSign)
      if this.isInside(result)
      if result != blockPos
    } yield result
  }

  /**
   * Returns the closest block inside the zone from the given blockPos
   */
  def closestCoordinateInside(blockPos: Vec3i) =
    Vec3i(
      clamp(blockPos.getX, this.minX, this.maxX),
      clamp(blockPos.getY, this.minY, this.maxY),
      clamp(blockPos.getZ, this.minZ, this.maxZ)
    )

  /**
   * Returns a triangular plane of coordinates that all share the same manhattan distance from blockPos
   */
  private def combinationsOfThreeThatSumTo(d: Int) =
    for {
      a <- 0 to d
      b <- 0 to (d - a)
      c = d - a - b
      if c >= 0
    } yield (a, b, c)

  def volume = this.getXSpan * this.getYSpan * this.getZSpan
}
