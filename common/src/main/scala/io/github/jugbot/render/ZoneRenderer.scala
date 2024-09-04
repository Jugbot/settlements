package io.github.jugbot.render

import com.mojang.blaze3d.vertex.PoseStack
import io.github.jugbot.entity.zone.ZoneEntity
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.{EntityRenderer, EntityRendererProvider}
import net.minecraft.client.renderer.{LevelRenderer, MultiBufferSource, RenderType}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.shapes.Shapes

case class RGBA(r: Float, g: Float, b: Float, a: Float)

class ZoneRenderer(context: EntityRendererProvider.Context)
    extends EntityRenderer[ZoneEntity](
      context
    ) {

  override def getTextureLocation(fae: ZoneEntity): ResourceLocation =
    null

  override def shouldRender(entity: ZoneEntity, frustum: Frustum, d: Double, e: Double, f: Double): Boolean = true

  override def render(entity: ZoneEntity,
                      f: Float,
                      g: Float,
                      poseStack: PoseStack,
                      multiBufferSource: MultiBufferSource,
                      i: Int
  ): Unit =
    val vertexConsumer = multiBufferSource.getBuffer(RenderType.lines)
    val bb = entity.getBoundingBox
    val voxelShape = Shapes.create(bb)
    val color = RGBA(1.0f, 0.0f, 0.0f, 1.0f)
    val center = bb.getCenter
    LevelRenderer.renderVoxelShape(poseStack,
                                   vertexConsumer,
                                   voxelShape,
                                   -center.x,
                                   -center.y,
                                   -center.z,
                                   color.r,
                                   color.g,
                                   color.b,
                                   color.a,
                                   true
    )
    super.render(entity, f, g, poseStack, multiBufferSource, i)
}
