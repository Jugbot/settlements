package io.github.jugbot.block

import io.github.jugbot.blockentity.ShrineBlockEntity
import io.github.jugbot.screen.ShrineScreen
import net.fabricmc.api.{EnvType, Environment}
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock.createTickerHelper
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityTicker, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState}
import net.minecraft.world.level.block.{BaseEntityBlock, RenderShape, SoundType}
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.{InteractionHand, InteractionResult}

class ShrineBlock(properties: Properties) extends BaseEntityBlock(properties) {

  override def newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity =
    new ShrineBlockEntity(blockPos, blockState)

  override def getRenderShape(blockState: BlockState): RenderShape = RenderShape.MODEL

  override def use(blockState: BlockState,
                   level: Level,
                   blockPos: BlockPos,
                   player: Player,
                   interactionHand: InteractionHand,
                   blockHitResult: BlockHitResult
  ): InteractionResult =
    val blockEntity: BlockEntity = level.getBlockEntity(blockPos)
    blockEntity match
      case entity: ShrineBlockEntity if player.canUseGameMasterBlocks =>
        handlePlayerOpenMenu(player, entity)
        InteractionResult.sidedSuccess(level.isClientSide)
      case _ => InteractionResult.PASS

  @Environment(EnvType.CLIENT)
  private def handlePlayerOpenMenuClient(blockEntity: ShrineBlockEntity): Unit =
    Minecraft.getInstance().setScreen(new ShrineScreen(blockEntity))

  private def handlePlayerOpenMenu(player: Player, blockEntity: ShrineBlockEntity): Unit =
    if player.isLocalPlayer then {
      handlePlayerOpenMenuClient(blockEntity)
    } else {
      player
        .asInstanceOf[ServerPlayer]
        .connection
        .send(ClientboundBlockEntityDataPacket.create(blockEntity, (b: BlockEntity) => b.saveWithoutMetadata))
    }

  override def getTicker[T <: BlockEntity](level: Level,
                                           blockState: BlockState,
                                           blockEntityType: BlockEntityType[T]
  ): BlockEntityTicker[T] =
    createTickerHelper(blockEntityType, ShrineBlockEntity.TYPE.get(), ShrineBlockEntity.tick)

  override def setPlacedBy(level: Level,
                           blockPos: BlockPos,
                           blockState: BlockState,
                           livingEntity: LivingEntity,
                           itemStack: ItemStack
  ): Unit = {
    val blockEntity = level.getBlockEntity(blockPos)
    (blockEntity, livingEntity) match {
      case (shrineBlockEntity: ShrineBlockEntity, player: Player) =>
        shrineBlockEntity.owners += player.getUUID
      case _ => ()
    }
  }
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
