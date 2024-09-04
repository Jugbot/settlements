package io.github.jugbot.fabric

import io.github.jugbot.Mod
import net.fabricmc.api.ModInitializer

class ExampleModFabric extends ModInitializer {
  override def onInitialize(): Unit =
    Mod.initialize()
}
