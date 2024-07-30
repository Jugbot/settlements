package io.github.jugbot.blockentity

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import io.github.jugbot.Mod
import io.github.jugbot.registry.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import java.util.function.Supplier
import com.google.common.base.Suppliers
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import io.github.jugbot.block.ShrineBlock
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import com.mojang.datafixers.types.Type
import net.minecraft.nbt.CompoundTag

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
