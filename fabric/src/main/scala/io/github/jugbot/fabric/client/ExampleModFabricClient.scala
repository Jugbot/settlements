package io.github.jugbot.fabric.client

import io.github.jugbot.Mod
import net.fabricmc.api.ClientModInitializer

final class ExampleModFabricClient extends ClientModInitializer {
  override def onInitializeClient(): Unit =
    Mod.Client.initializeClient()
}
