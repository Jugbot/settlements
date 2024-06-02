package io.github.jugbot.fabric;

import net.fabricmc.api.ModInitializer;

import io.github.jugbot.ExampleMod;


class ExampleModFabric extends ModInitializer {
    override def onInitialize(): Unit = {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        ExampleMod.init();
    }
}