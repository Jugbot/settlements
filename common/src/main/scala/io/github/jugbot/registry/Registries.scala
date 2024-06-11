package io.github.jugbot.registry

import dev.architectury.registry.level.entity.EntityAttributeRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.jugbot.Mod
import io.github.jugbot.entity.FaeEntity
import net.minecraft.world.entity.EntityType

object Registries {
  final val ENTITY_TYPES: DeferredRegister[EntityType[?]] =
    DeferredRegister.create(
      Mod.MOD_ID,
      net.minecraft.core.registries.Registries.ENTITY_TYPE
    );

  final val FAE_ENTITY: RegistrySupplier[EntityType[FaeEntity]] =
    ENTITY_TYPES.register("fae", FaeEntity.TYPE)

  def initialize() = {
    ENTITY_TYPES.register();
    EntityAttributeRegistry.register(
      FAE_ENTITY,
      () => FaeEntity.createAttributes()
    );
  }
}
