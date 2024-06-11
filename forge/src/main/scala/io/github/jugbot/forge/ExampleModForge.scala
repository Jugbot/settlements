package io.github.jugbot.forge;

import dev.architectury.platform.forge.EventBuses
import io.github.jugbot.Mod
import net.minecraftforge.fml.common.{Mod => ForgeMod}
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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
