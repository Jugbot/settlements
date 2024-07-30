package io.github.jugbot.blockentity

import com.google.common.base.Suppliers
import io.github.jugbot.block.ShrineBlock
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.{AbstractContainerMenu, CraftingMenu}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState

import java.util.function.Supplier

class ShrineBlockEntity(pos: BlockPos, state: BlockState)
    extends BlockEntity(ShrineBlockEntity.TYPE.get(), pos, state) {
  override def load(compoundTag: CompoundTag): Unit =
    super.load(compoundTag)
    // Load persisted data

  override def saveAdditional(compoundTag: CompoundTag): Unit = {
    // Persist data
  }
}

object ShrineBlockEntity {

  final val TYPE: Supplier[BlockEntityType[ShrineBlockEntity]] = Suppliers.memoize(() =>
    BlockEntityType.Builder
      .of[ShrineBlockEntity](new ShrineBlockEntity(_, _), ShrineBlock.INSTANCE)
      // TODO: what should this be if not null?
      .build(null)
  )

}
