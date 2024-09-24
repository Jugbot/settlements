package io.github.jugbot.registry

import dev.architectury.registry.ReloadListenerRegistry
import dev.architectury.registry.level.entity.EntityAttributeRegistry
import dev.architectury.registry.registries.{DeferredRegister, RegistrySupplier}
import io.github.jugbot.Mod
import io.github.jugbot.ai.tree.FaeBehaviorTree
import io.github.jugbot.block.ShrineBlock
import io.github.jugbot.blockentity.ShrineBlockEntity
import io.github.jugbot.entity.FaeEntity
import io.github.jugbot.entity.zone.{SettlementZoneEntity, ShrineZoneEntity}
import io.github.jugbot.item.ShrineBlockItem
import net.minecraft.core.registries.Registries as MojRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

object Registries {
  final private val BLOCKS =
    DeferredRegister.create(
      Mod.MOD_ID,
      MojRegistries.BLOCK
    );
  final private val ITEMS =
    DeferredRegister.create(
      Mod.MOD_ID,
      MojRegistries.ITEM
    );
  final private val ENTITY_TYPES =
    DeferredRegister.create(
      Mod.MOD_ID,
      MojRegistries.ENTITY_TYPE
    );
  final private val BLOCK_ENTITY_TYPES = DeferredRegister.create(
    Mod.MOD_ID,
    MojRegistries.BLOCK_ENTITY_TYPE
  )

  final val FAE_ENTITY: RegistrySupplier[EntityType[FaeEntity]] =
    ENTITY_TYPES.register("fae", FaeEntity.TYPE)

  final val SHRINE_ZONE_ENTITY: RegistrySupplier[EntityType[ShrineZoneEntity]] =
    ENTITY_TYPES.register("shrine_zone", ShrineZoneEntity.TYPE)

  final val SETTLEMENT_ZONE_ENTITY: RegistrySupplier[EntityType[SettlementZoneEntity]] =
    ENTITY_TYPES.register("settlement_zone", SettlementZoneEntity.TYPE)

  final val SHRINE_BLOCK: RegistrySupplier[Block] = BLOCKS.register("shrine", () => ShrineBlock.INSTANCE)

  final val SHRINE_BLOCK_ITEM: RegistrySupplier[Item] =
    ITEMS.register("shrine", () => new ShrineBlockItem())

  final val SHRINE_BLOCK_ENTITY: RegistrySupplier[BlockEntityType[ShrineBlockEntity]] =
    BLOCK_ENTITY_TYPES.register("shrine", ShrineBlockEntity.TYPE)

  def initialize(): Unit = {
    ITEMS.register()
    BLOCKS.register()
    ENTITY_TYPES.register()
    BLOCK_ENTITY_TYPES.register()
    EntityAttributeRegistry.register(
      FAE_ENTITY,
      () => FaeEntity.createAttributes()
    )
    ReloadListenerRegistry.register(PackType.SERVER_DATA,
                                    FaeBehaviorTree.Loader,
                                    new ResourceLocation(Mod.MOD_ID, "behavior")
    )
  }

}
