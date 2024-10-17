package io.github.jugbot.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.utils.GameInstance
import io.github.jugbot.entity.zone.{ZoneEntity, ZoneType}
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.{EntityRenderer, EntityRendererProvider}
import net.minecraft.client.renderer.{LevelRenderer, MultiBufferSource, RenderType}
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.shapes.Shapes

case class RGBA(r: Float, g: Float, b: Float, a: Float)

class ZoneRenderer(context: EntityRendererProvider.Context)
    extends EntityRenderer[ZoneEntity](
      context
    ) {

  override def getTextureLocation(entity: ZoneEntity): ResourceLocation =
    null

  override def shouldRender(entity: ZoneEntity, frustum: Frustum, d: Double, e: Double, f: Double): Boolean =
    GameInstance.getClient.getEntityRenderDispatcher.shouldRenderHitBoxes

  override def render(entity: ZoneEntity,
                      f: Float,
                      g: Float,
                      poseStack: PoseStack,
                      multiBufferSource: MultiBufferSource,
                      i: Int
  ): Unit =
    val bb = entity.getBoundingBox
    val voxelShape = Shapes.create(bb)
    val color = entity.getZoneType match {
      case ZoneType.Settlement => RGBA(0.0f, 0.0f, 1.0f, 0.2f)
      case ZoneType.Zoning     => RGBA(0.5f, 0.5f, 1.0f, 0.2f)
      case _                   => RGBA(1.0f, 1.0f, 1.0f, 0.2f)
    }
    val center = bb.getCenter

    LevelRenderer.renderVoxelShape(
      poseStack,
      multiBufferSource.getBuffer(RenderType.lines),
      voxelShape,
      -center.x,
      -center.y,
      -center.z,
      color.r,
      color.g,
      color.b,
      1f,
      true
    )
    LevelRenderer.addChainedFilledBoxVertices(
      poseStack,
      multiBufferSource.getBuffer(RenderType.debugFilledBox),
      bb.minX.toFloat - center.x,
      bb.minY.toFloat - center.y,
      bb.minZ.toFloat - center.z,
      bb.maxX.toFloat - center.x,
      bb.maxY.toFloat - center.y,
      bb.maxZ.toFloat - center.z,
      color.r,
      color.g,
      color.b,
      color.a
    )

    this.renderNameTag(entity, Component.literal(entity.getZoneType.toString), poseStack, multiBufferSource, i)
}
