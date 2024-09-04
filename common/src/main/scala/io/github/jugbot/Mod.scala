package io.github.jugbot

import dev.architectury.registry.client.level.entity.{EntityModelLayerRegistry, EntityRendererRegistry}
import io.github.jugbot.model.FaeEntityModel
import io.github.jugbot.registry.Registries
import io.github.jugbot.render.{FaeRenderer, ZoneRenderer}
import net.fabricmc.api.{EnvType, Environment}
import org.apache.logging.log4j.{LogManager, Logger}

object Mod {
  final val MOD_ID = "settlements"

  val LOGGER: Logger = LogManager.getLogger

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
      EntityRendererRegistry.register(
        Registries.SHRINE_ZONE_ENTITY,
        new ZoneRenderer(_)
      )
      EntityRendererRegistry.register(
        Registries.SETTLEMENT_ZONE_ENTITY,
        new ZoneRenderer(_)
      )
    }
  }
}
