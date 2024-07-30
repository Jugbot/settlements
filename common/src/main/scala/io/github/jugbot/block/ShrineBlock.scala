package io.github.jugbot.block

import io.github.jugbot.blockentity.ShrineBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState}
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.block.{BaseEntityBlock, RenderShape, SoundType}
import net.minecraft.world.level.material.MapColor

class ShrineBlock(properties: Properties) extends BaseEntityBlock(properties) {

  override def newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity =
    new ShrineBlockEntity(blockPos, blockState)

  override def getRenderShape(blockState: BlockState): RenderShape = RenderShape.MODEL
}

object ShrineBlock {
  val INSTANCE = new ShrineBlock(
    BlockBehaviour.Properties.of
      .mapColor(MapColor.WOOD)
      .instrument(NoteBlockInstrument.BASS)
      .strength(2.5f)
      .sound(SoundType.WOOD)
  )
}
