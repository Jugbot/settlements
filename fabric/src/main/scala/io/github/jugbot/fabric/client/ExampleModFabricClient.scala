package io.github.jugbot.fabric.client

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry
import dev.architectury.registry.client.level.entity.EntityRendererRegistry
import io.github.jugbot.Mod
import io.github.jugbot.model.FaeEntityModel
import io.github.jugbot.registry.Registries
import io.github.jugbot.render.FaeRenderer
import net.fabricmc.api.ClientModInitializer

final class ExampleModFabricClient extends ClientModInitializer {
  override def onInitializeClient(): Unit =
    Mod.Client.initializeClient()
}
