package io.github.jugbot.extension

import net.minecraft.core.Vec3i

object Vec3i {
  extension (vec: Vec3i) {
    def chebyshevDistance(other: Vec3i): Int =
      (vec.getX - other.getX).abs.max((vec.getY - other.getY).abs).max((vec.getZ - other.getZ).abs)
  }
}
