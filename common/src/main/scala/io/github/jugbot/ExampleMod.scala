package io.github.jugbot;

import org.apache.logging.log4j.LogManager
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry
import io.github.jugbot.registry.Registries
import net.minecraft.client.renderer.entity.CowRenderer
import io.github.jugbot.render.FaeRenderer

object ExampleMod {
    final val MOD_ID = "settlements";

    private val LOGGER = LogManager.getLogger

    def init() = {
        LOGGER.info("Hello World!")
        Registries.initialize()
    }
    
    @Environment(EnvType.CLIENT)
    object Client {
        @Environment(EnvType.CLIENT)
        def initializeClient(): Unit = {
            EntityRendererRegistry.register(Registries.FAE_ENTITY, FaeRenderer.apply(_));
        }
    }
}
