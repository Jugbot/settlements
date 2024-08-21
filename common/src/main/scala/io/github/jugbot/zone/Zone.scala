package io.github.jugbot.zone

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.{AABB, Vec3}

class Zone(xA: Double, yA: Double, zA: Double, xB: Double, yB: Double, zB: Double)
    extends AABB(xA, yA, zA, xB, yB, zB) {

  def this(blockPos: BlockPos) =
    this(blockPos.getX.toDouble,
         blockPos.getY.toDouble,
         blockPos.getZ.toDouble,
         (blockPos.getX + 1).toDouble,
         (blockPos.getY + 1).toDouble,
         (blockPos.getZ + 1).toDouble
    )

  def this(blockPos: BlockPos, blockPos2: BlockPos) =
    this(blockPos.getX.toDouble,
         blockPos.getY.toDouble,
         blockPos.getZ.toDouble,
         blockPos2.getX.toDouble,
         blockPos2.getY.toDouble,
         blockPos2.getZ.toDouble
    )

  def this(vec3: Vec3, vec32: Vec3) =
    this(vec3.x, vec3.y, vec3.z, vec32.x, vec32.y, vec32.z)

}
