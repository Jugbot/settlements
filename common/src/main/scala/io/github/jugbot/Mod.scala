package io.github.jugbot

import dev.architectury.registry.ReloadListenerRegistry
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry
import dev.architectury.registry.client.level.entity.EntityRendererRegistry
import io.github.jugbot.model.FaeEntityModel
import io.github.jugbot.registry.Registries
import io.github.jugbot.render.FaeRenderer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.apache.logging.log4j.LogManager

object Mod {
  final val MOD_ID = "settlements"

  val LOGGER = LogManager.getLogger

  def initialize(): Unit = {
    LOGGER.info("Hello World!")
    Registries.initialize()
  }

  @Environment(EnvType.CLIENT)
  object Client {
    @Environment(EnvType.CLIENT)
    def initializeClient(): Unit = {
      EntityModelLayerRegistry.register(
        FaeEntityModel.LAYER_LOCATION,
        () => FaeEntityModel.createBodyLayer()
      )
      EntityRendererRegistry.register(
        Registries.FAE_ENTITY,
        new FaeRenderer(_)
      )
    }
  }
}
