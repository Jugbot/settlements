package io.github.jugbot.registry

import com.fasterxml.jackson.databind.JavaType
import dev.architectury.registry.ReloadListenerRegistry
import dev.architectury.registry.level.entity.EntityAttributeRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.jugbot.Mod
import io.github.jugbot.ai.tree.{FaeBehavior, FaeBehaviorTree}
import io.github.jugbot.ai.{BTMapper, Node}
import io.github.jugbot.entity.FaeEntity
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.{ResourceManager, SimplePreparableReloadListener}
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.entity.EntityType

object Registries {
  final private val ENTITY_TYPES: DeferredRegister[EntityType[?]] =
    DeferredRegister.create(
      Mod.MOD_ID,
      net.minecraft.core.registries.Registries.ENTITY_TYPE
    );
  final private val BEHAVIORS = DeferredRegister.create(
    Mod.MOD_ID,
    net.minecraft.core.registries.Registries.ENTITY_TYPE
  );

  final val FAE_ENTITY: RegistrySupplier[EntityType[FaeEntity]] =
    ENTITY_TYPES.register("fae", FaeEntity.TYPE)

  def initialize(): Unit = {
    ENTITY_TYPES.register()
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
