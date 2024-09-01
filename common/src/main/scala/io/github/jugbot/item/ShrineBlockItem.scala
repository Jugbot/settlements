package io.github.jugbot.item

import io.github.jugbot.block.ShrineBlock
import io.github.jugbot.entity.zone.{CollisionLayer, SettlementZoneEntity, ZoneManager}
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.{BlockItem, Item}
import net.minecraft.world.level.block.state.BlockState

class ShrineBlockItem extends BlockItem(ShrineBlock.INSTANCE, Item.Properties()) {

  override def place(blockPlaceContext: BlockPlaceContext): InteractionResult =
    val result = super.place(blockPlaceContext)
    val level = blockPlaceContext.getLevel
    if result.consumesAction() && !level.isClientSide then
      val blockPos = blockPlaceContext.getClickedPos
      val shrineZoneEntity =
        SettlementZoneEntity.TYPE.get.spawn(level.asInstanceOf[ServerLevel], blockPos, MobSpawnType.COMMAND)
    result

  override def canPlace(blockPlaceContext: BlockPlaceContext, blockState: BlockState): Boolean =
    val level = blockPlaceContext.getLevel
    val blockPos = blockPlaceContext.getClickedPos
    val prospectiveSettlementBB =
      SettlementZoneEntity.TYPE.get().getAABB(blockPos.getCenter.x, blockPos.getCenter.y, blockPos.getCenter.z)

    val canPlaceSettlement = ZoneManager.canFitAt(level, prospectiveSettlementBB, CollisionLayer.Settlement)

    // TODO: Adopting orphaned settlement zone
    super.canPlace(blockPlaceContext, blockState) && canPlaceSettlement
}
