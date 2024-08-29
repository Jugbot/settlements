package io.github.jugbot.zone

import io.github.jugbot.UnitSuite
import net.minecraft.core.Vec3i

class ZoneTest extends UnitSuite {

  test("closestBlocksInside should return all blocks inside the zone") {
    val zone = new Zone(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(1, 1, 1)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result.length shouldEqual 4 * 4 * 4 - 1
  }

  test("closestBlocksInside should return blocks ordered by increasing distance from blockPos") {
    val zone = new Zone(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(2, 2, 2)
    val result = zone.closestCoordinatesInside(blockPos)
    var distance = 1
    for {
      i <- result.indices
    } {
      val coordDist = blockPos.distManhattan(result(i))
      withClue(s"Coordinate manhattan distance was not strictly increasing from ${i - 1} ${if i > 0 then result(i - 1) else "start"} to $i ${result(i)}") {
        coordDist should be >= distance
      }
      distance = coordDist
    }
  }

  test("closestBlocksInside should handle edge case where blockPos is outside the zone") {
    val zone = new Zone(0, 0, 0, 5, 5, 5)
    val blockPos = new Vec3i(10, 10, 10)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result.head shouldEqual Vec3i(5, 5, 5)
  }

  test("closestBlocksInside should handle edge case where blockPos is on the boundary of the zone") {
    val zone = new Zone(0, 0, 0, 5, 5, 5)
    val blockPos = new Vec3i(6, 6, 6)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result.head shouldEqual Vec3i(5, 5, 5)
  }

  test("closestBlocksInside should handle edge case where zone is a single block") {
    val zone = new Zone(2, 2, 2, 2, 2, 2)
    val blockPos = new Vec3i(2, 2, 2)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result shouldEqual Nil
  }

  test("closestBlocksInside should handle edge case where zone is a single block and blockPos is outside") {
    val zone = new Zone(2, 2, 2, 2, 2, 2)
    val blockPos = new Vec3i(20, 20, 20)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result shouldEqual Seq(Vec3i(2, 2, 2))
  }
}
