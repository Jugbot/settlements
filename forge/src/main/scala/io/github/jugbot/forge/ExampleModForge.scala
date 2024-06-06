package io.github.jugbot.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod as ForgeMod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import io.github.jugbot.Mod;

@ForgeMod(Mod.MOD_ID)
object ExampleModForge {
  {
    // Submit our event bus to let Architectury API register our content on the right time.
    EventBuses.registerModEventBus(
      Mod.MOD_ID,
      FMLJavaModLoadingContext.get().getModEventBus()
    );

    // Run our common setup.
    Mod.initialize();
  }
}
