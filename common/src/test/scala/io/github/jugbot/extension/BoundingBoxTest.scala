package io.github.jugbot.extension

import io.github.jugbot.UnitSuite
import io.github.jugbot.extension.BoundingBox.*
import io.github.jugbot.extension.Vec3i.chebyshevDistance
import net.minecraft.core.Vec3i
import net.minecraft.world.level.levelgen.structure.BoundingBox

class ClosestBlocksInside extends UnitSuite {
  test("closestBlocksInside should return all blocks inside the zone") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(0, 0, 0)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result.length shouldEqual 4 * 4 * 4
  }

  test("closestBlocksInside should return blocks inside the zone within some distance") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(1, 1, 1)
    val result = zone.closestCoordinatesInside(blockPos, 1).toList

    result.length shouldEqual 3 * 3 * 3
  }

  test("closestBlocksInside should only return blockPos with max distance set to zero") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(1, 1, 1)
    val result = zone.closestCoordinatesInside(blockPos, 0).toList

    result.length shouldEqual 1
  }

  test("closestBlocksInside should return blocks ordered by increasing distance from blockPos") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val blockPos = new Vec3i(2, 2, 2)
    val result = zone.closestCoordinatesInside(blockPos).toList
    var distance = 0
    for {
      i <- result.indices
    } {
      val coordDist = blockPos.chebyshevDistance(result(i))
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

  test("closestBlocksInside should handle edge case where zone is a single block and blockPos is inside") {
    val zone = new BoundingBox(2, 2, 2, 2, 2, 2)
    val blockPos = new Vec3i(2, 2, 2)
    val result = zone.closestCoordinatesInside(blockPos).toList

    result shouldEqual Seq(Vec3i(2, 2, 2))
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
    val _ = zone.closestCoordinatesInside(blockPos).next()
    val usedMemoryAfter = usedMemory

    val resultUsedMemory = usedMemoryAfter - usedMemoryBefore
    resultUsedMemory shouldEqual 0L
  }

  test("closestBlocksInside should handle max integer zone size") {
    val zone = new BoundingBox(0, 0, 0, Int.MaxValue, Int.MaxValue, Int.MaxValue)
    val blockPos = new Vec3i(0, 0, 0)

    assertThrows[ArithmeticException] {
      zone.closestCoordinatesInside(blockPos)
    }
  }
}

class Corners extends UnitSuite {
  test("corners should return the 8 corners of the bb") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val result = zone.corners

    result should contain theSameElementsAs Seq(
      new Vec3i(0, 0, 0),
      new Vec3i(0, 0, 3),
      new Vec3i(0, 3, 0),
      new Vec3i(0, 3, 3),
      new Vec3i(3, 0, 0),
      new Vec3i(3, 0, 3),
      new Vec3i(3, 3, 0),
      new Vec3i(3, 3, 3)
    )
  }

  test("corners should return the single corner of a single coordinate bb") {
    val zone = new BoundingBox(0, 0, 0, 0, 0, 0)
    val result = zone.corners

    result should contain theSameElementsAs Seq(
      new Vec3i(0, 0, 0)
    )
  }

  test("corners should return the 2 corners of a line") {
    val zone = new BoundingBox(0, 0, 0, 2, 0, 0)
    val result = zone.corners

    result should contain theSameElementsAs Seq(
      new Vec3i(0, 0, 0),
      new Vec3i(2, 0, 0)
    )
  }

  test("corners should return the 4 corners of a plane") {
    val zone = new BoundingBox(0, 0, 0, 2, 2, 0)
    val result = zone.corners

    result should contain theSameElementsAs Seq(
      new Vec3i(0, 0, 0),
      new Vec3i(0, 2, 0),
      new Vec3i(2, 0, 0),
      new Vec3i(2, 2, 0)
    )
  }
}

class Edges extends UnitSuite {
  test("edges should return the appropriate coordinates") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val result = zone.edges.flatMap(_.coordinatesInside)

    result.length shouldEqual 4 * 8
  }

  test("edges should return the appropriate coordinates for a single block zone") {
    val zone = new BoundingBox(0, 0, 0, 0, 0, 0)
    val result = zone.edges.flatMap(_.coordinatesInside)

    result should contain theSameElementsAs Seq(
      new Vec3i(0, 0, 0)
    )
  }

  test("edges should return the appropriate coordinates for a 2x2x2 zone") {
    val zone = new BoundingBox(0, 0, 0, 1, 1, 1)
    val result = zone.edges.flatMap(_.coordinatesInside)

    result should contain theSameElementsAs Seq(
      new Vec3i(0, 0, 0),
      new Vec3i(0, 0, 1),
      new Vec3i(0, 1, 0),
      new Vec3i(0, 1, 1),
      new Vec3i(1, 0, 0),
      new Vec3i(1, 0, 1),
      new Vec3i(1, 1, 0),
      new Vec3i(1, 1, 1)
    )
  }
}

class Sides extends UnitSuite {
  test("sides returns the correct number of blocks") {
    val zone = new BoundingBox(0, 0, 0, 3, 3, 3)
    val result = zone.sides.flatMap(_.coordinatesInside).toList

    result.length shouldEqual 4 * 4 * 4 - 2 * 2 * 2
  }

  test("sides returns the correct number of blocks for a single block zone") {
    val zone = new BoundingBox(0, 0, 0, 0, 0, 0)
    val result = zone.sides.flatMap(_.coordinatesInside).toList

    result.length shouldEqual 1
  }

  test("sides returns the correct number of blocks for a 2x2x2 zone") {
    val zone = new BoundingBox(0, 0, 0, 1, 1, 1)
    val result = zone.sides.flatMap(_.coordinatesInside).toList

    result.length shouldEqual 8
  }

  test("sides returns the correct number of blocks for a 3x3x3 zone") {
    val zone = new BoundingBox(0, 0, 0, 2, 2, 2)
    val result = zone.sides.flatMap(_.coordinatesInside).toList

    result.length shouldEqual 3 * 3 * 3 - 1
  }

  test("sides returns the correct number of blocks for a 3x3x1 zone") {
    val zone = new BoundingBox(0, 0, 0, 2, 2, 0)
    val result = zone.sides.flatMap(_.coordinatesInside).toList

    result.length shouldEqual 9
  }
}
