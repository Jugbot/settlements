package io.github.jugbot.model

import com.google.common.collect.{ImmutableList, Iterables}
import com.mojang.blaze3d.vertex.PoseStack
import io.github.jugbot.Mod
import io.github.jugbot.entity.FaeEntity
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.{ModelLayerLocation, ModelPart, PartPose}
import net.minecraft.client.model.geom.builders.{CubeDeformation, CubeListBuilder, LayerDefinition, MeshDefinition}
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.{EquipmentSlot, HumanoidArm}

import java.lang

class FaeEntityModel(modelPart: ModelPart)
    extends HumanoidModel[FaeEntity](modelPart, (r: ResourceLocation) => RenderType.entityTranslucent(r)) {
  private val parts = modelPart.getAllParts.filter((modelPartx: ModelPart) => !modelPartx.isEmpty).toList
  private val leftSleeve = modelPart.getChild("left_sleeve")
  private val rightSleeve = modelPart.getChild("right_sleeve")
  private val leftPants = modelPart.getChild("left_pants")
  private val rightPants = modelPart.getChild("right_pants")
  private val jacket = modelPart.getChild("jacket")
  private val cloak = modelPart.getChild("cloak")
  private val ear = modelPart.getChild("ear")
  private val slim = false

  override protected def bodyParts: lang.Iterable[ModelPart] = Iterables.concat(
    super.bodyParts(),
    ImmutableList.of(this.leftPants, this.rightPants, this.leftSleeve, this.rightSleeve, this.jacket)
  )

  override def setupAnim(livingEntity: FaeEntity, f: Float, g: Float, h: Float, i: Float, j: Float): Unit = {
    super.setupAnim(livingEntity, f, g, h, i, j)
    this.leftPants.copyFrom(this.leftLeg)
    this.rightPants.copyFrom(this.rightLeg)
    this.leftSleeve.copyFrom(this.leftArm)
    this.rightSleeve.copyFrom(this.rightArm)
    this.jacket.copyFrom(this.body)
    if livingEntity.getItemBySlot(EquipmentSlot.CHEST).isEmpty then
      if livingEntity.isCrouching then {
        this.cloak.z = 1.4f
        this.cloak.y = 1.85f
      } else {
        this.cloak.z = 0.0f
        this.cloak.y = 0.0f
      }
    else if livingEntity.isCrouching then {
      this.cloak.z = 0.3f
      this.cloak.y = 0.8f
    } else {
      this.cloak.z = -1.1f
      this.cloak.y = -0.85f
    }
  }

  override def setAllVisible(bl: Boolean): Unit = {
    super.setAllVisible(bl)
    this.leftSleeve.visible = bl
    this.rightSleeve.visible = bl
    this.leftPants.visible = bl
    this.rightPants.visible = bl
    this.jacket.visible = bl
    this.cloak.visible = bl
    this.ear.visible = bl
  }

  override def translateToHand(humanoidArm: HumanoidArm, poseStack: PoseStack): Unit = {
    val modelPart = this.getArm(humanoidArm)
    if this.slim then {
      val f = 0.5f * (if humanoidArm eq HumanoidArm.RIGHT then 1
                      else -1).toFloat
      modelPart.x += f
      modelPart.translateAndRotate(poseStack)
      modelPart.x -= f
    } else modelPart.translateAndRotate(poseStack)
  }

  def getRandomModelPart(randomSource: RandomSource): ModelPart = this.parts.get(randomSource.nextInt(this.parts.size))
}

object FaeEntityModel {
  private val EAR = "ear"
  private val CLOAK = "cloak"
  private val LEFT_SLEEVE = "left_sleeve"
  private val RIGHT_SLEEVE = "right_sleeve"
  private val LEFT_PANTS = "left_pants"
  private val RIGHT_PANTS = "right_pants"

  val LAYER_LOCATION: ModelLayerLocation =
    new ModelLayerLocation(new ResourceLocation(Mod.MOD_ID, "fae"), "main");

  def createBodyLayer(): LayerDefinition = {
    val meshdefinition: MeshDefinition = createMesh(CubeDeformation.NONE, false)

    LayerDefinition.create(meshdefinition, 64, 64)
  }

  def createMesh(cubeDeformation: CubeDeformation, bl: Boolean): MeshDefinition = {
    val meshDefinition = HumanoidModel.createMesh(cubeDeformation, 0.0f)
    val partDefinition = meshDefinition.getRoot
    partDefinition.addOrReplaceChild(
      "ear",
      CubeListBuilder.create.texOffs(24, 0).addBox(-3.0f, -6.0f, -1.0f, 6.0f, 6.0f, 1.0f, cubeDeformation),
      PartPose.ZERO
    )
    partDefinition.addOrReplaceChild(
      "cloak",
      CubeListBuilder.create.texOffs(0, 0).addBox(-5.0f, 0.0f, -1.0f, 10.0f, 16.0f, 1.0f, cubeDeformation, 1.0f, 0.5f),
      PartPose.offset(0.0f, 0.0f, 0.0f)
    )
    val f = 0.25f
    if bl then {
      partDefinition.addOrReplaceChild(
        "left_arm",
        CubeListBuilder.create.texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f, cubeDeformation),
        PartPose.offset(5.0f, 2.5f, 0.0f)
      )
      partDefinition.addOrReplaceChild(
        "right_arm",
        CubeListBuilder.create.texOffs(40, 16).addBox(-2.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f, cubeDeformation),
        PartPose.offset(-5.0f, 2.5f, 0.0f)
      )
      partDefinition.addOrReplaceChild(
        "left_sleeve",
        CubeListBuilder.create
          .texOffs(48, 48)
          .addBox(-1.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f, cubeDeformation.extend(0.25f)),
        PartPose.offset(5.0f, 2.5f, 0.0f)
      )
      partDefinition.addOrReplaceChild(
        "right_sleeve",
        CubeListBuilder.create
          .texOffs(40, 32)
          .addBox(-2.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f, cubeDeformation.extend(0.25f)),
        PartPose.offset(-5.0f, 2.5f, 0.0f)
      )
    } else {
      partDefinition.addOrReplaceChild(
        "left_arm",
        CubeListBuilder.create.texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, cubeDeformation),
        PartPose.offset(5.0f, 2.0f, 0.0f)
      )
      partDefinition.addOrReplaceChild(
        "left_sleeve",
        CubeListBuilder.create
          .texOffs(48, 48)
          .addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, cubeDeformation.extend(0.25f)),
        PartPose.offset(5.0f, 2.0f, 0.0f)
      )
      partDefinition.addOrReplaceChild(
        "right_sleeve",
        CubeListBuilder.create
          .texOffs(40, 32)
          .addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, cubeDeformation.extend(0.25f)),
        PartPose.offset(-5.0f, 2.0f, 0.0f)
      )
    }
    partDefinition.addOrReplaceChild(
      "left_leg",
      CubeListBuilder.create.texOffs(16, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, cubeDeformation),
      PartPose.offset(1.9f, 12.0f, 0.0f)
    )
    partDefinition.addOrReplaceChild(
      "left_pants",
      CubeListBuilder.create
        .texOffs(0, 48)
        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, cubeDeformation.extend(0.25f)),
      PartPose.offset(1.9f, 12.0f, 0.0f)
    )
    partDefinition.addOrReplaceChild(
      "right_pants",
      CubeListBuilder.create
        .texOffs(0, 32)
        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, cubeDeformation.extend(0.25f)),
      PartPose.offset(-1.9f, 12.0f, 0.0f)
    )
    partDefinition.addOrReplaceChild("jacket",
                                     CubeListBuilder.create
                                       .texOffs(16, 32)
                                       .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, cubeDeformation.extend(0.25f)),
                                     PartPose.ZERO
    )
    meshDefinition
  }
}
