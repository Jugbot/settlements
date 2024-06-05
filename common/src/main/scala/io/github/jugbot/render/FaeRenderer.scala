package io.github.jugbot.render

import io.github.jugbot.entity.FaeEntity
import io.github.jugbot.model.FaeEntityModel
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import io.github.jugbot.Mod

class FaeRenderer(context: EntityRendererProvider.Context)
    extends LivingEntityRenderer[FaeEntity, FaeEntityModel](
      context,
      new FaeEntityModel(context.bakeLayer(FaeEntityModel.LAYER_LOCATION)),
      0.5f
    ) {

    override def getTextureLocation(fae: FaeEntity): ResourceLocation = {
        return FaeRenderer.FAE_LOCATION;
    }
  }

object FaeRenderer {
  val FAE_LOCATION: ResourceLocation = new ResourceLocation(Mod.MOD_ID, "textures/entity/fae/fae.png");
}