package io.github.jugbot.fabric.gametest

import io.github.jugbot.entity.FaeEntity
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.{GameTest, GameTestHelper}

class FaeTest extends FabricGameTest {
  @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
  def doesSpawn(helper: GameTestHelper): Unit = {
    val spawnPos = new BlockPos(0,0,0)
    val entity = helper.spawn(FaeEntity.TYPE.get(), spawnPos)
    helper.assertEntityInstancePresent(entity, spawnPos)
    helper.succeed()
  }
}
