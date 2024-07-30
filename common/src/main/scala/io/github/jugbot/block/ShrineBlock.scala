package io.github.jugbot.block

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import io.github.jugbot.blockentity.ShrineBlockEntity
import java.util.function.Supplier
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Blocks

class ShrineBlock(properties: Properties) extends BaseEntityBlock(properties) {

  override def newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity =
    new ShrineBlockEntity(blockPos, blockState)
}

object ShrineBlock {
  val INSTANCE = new ShrineBlock(BlockBehaviour.Properties.copy(Blocks.STONE))
}
