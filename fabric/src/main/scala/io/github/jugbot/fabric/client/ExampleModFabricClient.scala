package io.github.jugbot.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry
import dev.architectury.registry.client.level.entity.EntityRendererRegistry
import io.github.jugbot.model.FaeEntityModel
import io.github.jugbot.registry.Registries
import io.github.jugbot.render.FaeRenderer
import io.github.jugbot.Mod

final class ExampleModFabricClient extends ClientModInitializer {
    override def onInitializeClient(): Unit = {
      Mod.Client.initializeClient()
    }
}