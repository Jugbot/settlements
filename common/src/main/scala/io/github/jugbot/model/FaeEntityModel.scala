package io.github.jugbot.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import io.github.jugbot.Mod
import io.github.jugbot.entity.FaeEntity
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.geom.builders.PartDefinition
import net.minecraft.resources.ResourceLocation

class FaeEntityModel(root: ModelPart) extends EntityModel[FaeEntity] {
  final private val bb_main: ModelPart = root.getChild("bb_main")

  override def setupAnim(
    entity: FaeEntity,
    f: Float,
    g: Float,
    h: Float,
    i: Float,
    j: Float
  ): Unit = {}

  override def renderToBuffer(
    poseStack: PoseStack,
    vertexConsumer: VertexConsumer,
    packedLight: Int,
    packedOverlay: Int,
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float
  ): Unit =
    bb_main.render(
      poseStack,
      vertexConsumer,
      packedLight,
      packedOverlay,
      red,
      green,
      blue,
      alpha
    )
}

object FaeEntityModel {
  final val LAYER_LOCATION: ModelLayerLocation =
    new ModelLayerLocation(new ResourceLocation(Mod.MOD_ID, "fae"), "main");

  def createBodyLayer(): LayerDefinition = {
    val meshdefinition: MeshDefinition = new MeshDefinition()
    val partdefinition: PartDefinition = meshdefinition.getRoot

    val bb_main: PartDefinition = partdefinition.addOrReplaceChild(
      "bb_main",
      CubeListBuilder
        .create()
        .texOffs(0, 0)
        .addBox(
          -8.0f,
          -16.0f,
          -8.0f,
          16.0f,
          16.0f,
          16.0f,
          new CubeDeformation(0.0f)
        ),
      PartPose.offset(0.0f, 24.0f, 0.0f)
    )

    LayerDefinition.create(meshdefinition, 64, 64)
  }
}
