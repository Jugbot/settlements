package io.github.jugbot.fabric;

import io.github.jugbot.Mod
import net.fabricmc.api.ModInitializer;

class ExampleModFabric extends ModInitializer {
  override def onInitialize(): Unit = {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    // Run our common setup.
    Mod.initialize();
  }
}
