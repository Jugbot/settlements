package io.github.jugbot.extension

import io.github.jugbot.UnitSuite
import io.github.jugbot.extension.BoundingBox.closestCoordinatesInside
import net.minecraft.core.Vec3i
import net.minecraft.world.level.levelgen.structure.BoundingBox

class BoundingBoxTest extends UnitSuite {

  test("closestBlocksInside should return all blocks inside the zone") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(1, 1, 1)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result.length shouldEqual 4 * 4 * 4 - 1
  }

  test("closestBlocksInside should return blocks inside the zone within some distance") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(1, 1, 1)
    val result = zone.closestCoordinatesInside(blockPos, 1).toList

    result.length shouldEqual 6
  }

  test("closestBlocksInside should return no blocks within zero distance") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(1, 1, 1)
    val result = zone.closestCoordinatesInside(blockPos, 0).toList

    result.length shouldEqual 0
  }

  test("closestBlocksInside should return blocks ordered by increasing distance from blockPos") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(2, 2, 2)
    val result = zone.closestCoordinatesInside(blockPos)
    var distance = 1
    for {
      i <- result.indices
    } {
      val coordDist = blockPos.distManhattan(result(i))
      withClue(s"Coordinate manhattan distance was not strictly increasing from ${i - 1} ${
          if i > 0 then result(i - 1) else "start"
        } to $i ${result(i)}") {
        coordDist should be >= distance
      }
      distance = coordDist
    }
  }

  test("closestBlocksInside should handle edge case where blockPos is outside the zone") {
    val zone = new BoundingBox(0, 0, 0, 5, 5, 5)
    val blockPos = new Vec3i(10, 10, 10)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result.head shouldEqual Vec3i(5, 5, 5)
  }

  test("closestBlocksInside should handle edge case where blockPos is on the boundary of the zone") {
    val zone = new BoundingBox(0, 0, 0, 5, 5, 5)
    val blockPos = new Vec3i(6, 6, 6)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result.head shouldEqual Vec3i(5, 5, 5)
  }

  test("closestBlocksInside should handle edge case where zone is a single block") {
    val zone = new BoundingBox(2, 2, 2, 2, 2, 2)
    val blockPos = new Vec3i(2, 2, 2)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result shouldEqual Nil
  }

  test("closestBlocksInside should handle edge case where zone is a single block and blockPos is outside") {
    val zone = new BoundingBox(2, 2, 2, 2, 2, 2)
    val blockPos = new Vec3i(20, 20, 20)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result shouldEqual Seq(Vec3i(2, 2, 2))
  }

  test("closestBlocksInside should only compute what is needed") {
    def usedMemory = {
      val runtime = Runtime.getRuntime
      runtime.totalMemory - runtime.freeMemory
    }
    val zone = new BoundingBox(0, 0, 0, 1e2.toInt, 1e2.toInt, 1e2.toInt)
    val blockPos = new Vec3i(-1, -1, -1)
    val usedMemoryBefore = usedMemory
    // Should not calculate the entire zone of 1e6 blocks if we just need the first one
    val result = zone.closestCoordinatesInside(blockPos).head
    val usedMemoryAfter = usedMemory

    val resultUsedMemory = usedMemoryAfter - usedMemoryBefore
    resultUsedMemory shouldEqual 0L
    result shouldEqual Vec3i.ZERO
  }

  test("closestBlocksInside should handle max integer zone size") {
    val zone = new BoundingBox(0, 0, 0, Int.MaxValue, Int.MaxValue, Int.MaxValue)
    val blockPos = new Vec3i(0, 0, 0)

    assertThrows[ArithmeticException] {
      zone.closestCoordinatesInside(blockPos)
    }
  }
}
