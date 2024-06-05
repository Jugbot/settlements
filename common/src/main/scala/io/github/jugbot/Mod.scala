package io.github.jugbot;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry
import dev.architectury.registry.client.level.entity.EntityRendererRegistry
import io.github.jugbot.model.FaeEntityModel
import io.github.jugbot.registry.Registries
import io.github.jugbot.render.FaeRenderer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.entity.CowRenderer
import org.apache.logging.log4j.LogManager

object Mod {
    final val MOD_ID = "settlements";

    private val LOGGER = LogManager.getLogger

    def initialize() = {
        LOGGER.info("Hello World!")
        Registries.initialize()
    }
    
    @Environment(EnvType.CLIENT)
    object Client {
        @Environment(EnvType.CLIENT)
        def initializeClient(): Unit = {
            EntityModelLayerRegistry.register(FaeEntityModel.LAYER_LOCATION, FaeEntityModel.createBodyLayer _)
            EntityRendererRegistry.register(Registries.FAE_ENTITY, new FaeRenderer(_));
        }
    }
}
