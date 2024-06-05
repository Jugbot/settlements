package io.github.jugbot.model

import io.github.jugbot.entity.FaeEntity
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.PartDefinition
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.world.entity.Entity
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.PoseStack
import io.github.jugbot.ExampleMod

class FaeEntityModel(root: ModelPart) extends EntityModel[FaeEntity] {
	final val bb_main: ModelPart = root.getChild("bb_main");

	def createBodyLayer(): LayerDefinition = {
		val meshdefinition: MeshDefinition = new MeshDefinition();
		val partdefinition: PartDefinition = meshdefinition.getRoot();

		val bb_main: PartDefinition = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	override def setupAnim(entity: FaeEntity, f: Float, g: Float, h: Float, i: Float, j: Float): Unit = {}

	override def renderToBuffer(poseStack: PoseStack, vertexConsumer: VertexConsumer, packedLight: Int, packedOverlay: Int, red: Float, green: Float, blue: Float, alpha: Float) = {
		bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}

object FaeEntityModel {
	final val LAYER_LOCATION: ModelLayerLocation = new ModelLayerLocation(new ResourceLocation(ExampleMod.MOD_ID, "fae"), "main");
}
