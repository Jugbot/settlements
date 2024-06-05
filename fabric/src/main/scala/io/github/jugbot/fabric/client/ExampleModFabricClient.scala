package io.github.jugbot.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry
import dev.architectury.registry.client.level.entity.EntityRendererRegistry
import io.github.jugbot.model.FaeEntityModel
import io.github.jugbot.registry.Registries
import io.github.jugbot.render.FaeRenderer

final class ExampleModFabricClient extends ClientModInitializer {
    override def onInitializeClient(): Unit = {
      EntityModelLayerRegistry.register(FaeEntityModel.LAYER_LOCATION, FaeEntityModel.createBodyLayer _)
      EntityRendererRegistry.register(Registries.FAE_ENTITY, new FaeRenderer(_));
    }
}