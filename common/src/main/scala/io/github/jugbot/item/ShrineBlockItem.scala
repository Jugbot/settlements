package io.github.jugbot.item

import io.github.jugbot.block.ShrineBlock
import io.github.jugbot.entity.zone.{SettlementZoneEntity, ShrineZoneEntity, ZoneManager, ZoneType}
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.{BlockItem, Item}
import net.minecraft.world.level.block.state.BlockState

class ShrineBlockItem extends BlockItem(ShrineBlock.INSTANCE, Item.Properties()) {

  override def place(blockPlaceContext: BlockPlaceContext): InteractionResult =
    val result = super.place(blockPlaceContext)
    val level = blockPlaceContext.getLevel
    if result.consumesAction() then
      val blockPos = blockPlaceContext.getClickedPos
      ZoneManager.spawnWithRadius(level,
                                  SettlementZoneEntity(SettlementZoneEntity.TYPE.get, _),
                                  blockPos,
                                  SettlementZoneEntity.DEFAULT_RADIUS
      )
      ZoneManager.spawnWithRadius(level,
                                  ShrineZoneEntity(ShrineZoneEntity.TYPE.get, _),
                                  blockPos,
                                  ShrineZoneEntity.DEFAULT_RADIUS
      )
    result

  override def canPlace(blockPlaceContext: BlockPlaceContext, blockState: BlockState): Boolean =
    val level = blockPlaceContext.getLevel
    val blockPos = blockPlaceContext.getClickedPos
    val prospectiveSettlementBB = ZoneManager.aabbWithRadius(blockPos, SettlementZoneEntity.DEFAULT_RADIUS)

    val conflictingSettlements = ZoneManager.getConflicting(level, prospectiveSettlementBB, ZoneType.Settlement)

    // TODO: Adopting orphaned settlement zone
    if super.canPlace(blockPlaceContext, blockState) then
      blockPlaceContext.getPlayer match {
        case player: Player if level.isClientSide =>
          player.sendSystemMessage(
            Component.translatable("block.settlements.shrine.overlapping", conflictingSettlements.head.position())
          )
        case _ =>
      }
      conflictingSettlements.isEmpty
    else false
}
