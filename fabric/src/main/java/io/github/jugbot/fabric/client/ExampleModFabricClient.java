package io.github.jugbot.fabric.client;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import io.github.jugbot.Client;
import net.fabricmc.api.ClientModInitializer;

public class ExampleModFabricClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    Client.initializeClient();
  }
}
