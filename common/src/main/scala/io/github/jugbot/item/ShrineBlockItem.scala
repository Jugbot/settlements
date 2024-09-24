package io.github.jugbot.item

import io.github.jugbot.block.ShrineBlock
import io.github.jugbot.entity.zone.*
import io.github.jugbot.extension.AABB.withRadius
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
      val settlementZone = ZoneEntity.makeZone(SettlementZoneEntity.TYPE.get,
                                               level,
                                               withRadius(blockPos, SettlementZoneEntity.DEFAULT_RADIUS)
      )
      val shrineZone = settlementZone.flatMap(zone =>
        ZoneEntity.makeChildZone(zone, ShrineZoneEntity.TYPE.get, withRadius(blockPos, ShrineZoneEntity.DEFAULT_RADIUS))
      )
      if settlementZone.isEmpty || shrineZone.isEmpty then
        level.removeBlock(blockPos, false)
        settlementZone.foreach(_.discard())
        shrineZone.foreach(_.discard())
        return InteractionResult.FAIL
    result

  override def canPlace(blockPlaceContext: BlockPlaceContext, blockState: BlockState): Boolean =
    val level = blockPlaceContext.getLevel
    val blockPos = blockPlaceContext.getClickedPos
    val prospectiveSettlementBB = withRadius(blockPos.getCenter, SettlementZoneEntity.DEFAULT_RADIUS)

    val conflictingSettlements = ZoneManager.getConflicting(level, prospectiveSettlementBB, ZoneType.Settlement)

    // TODO: Adopting orphaned settlement zone
    if super.canPlace(blockPlaceContext, blockState) then
      blockPlaceContext.getPlayer match {
        case player: Player if player.isLocalPlayer && conflictingSettlements.nonEmpty =>
          player.sendSystemMessage(
            Component.translatable("block.settlements.shrine.overlapping", conflictingSettlements.head.position())
          )
        case _ =>
      }
      conflictingSettlements.isEmpty
    else false
}
