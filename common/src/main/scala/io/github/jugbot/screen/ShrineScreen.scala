package io.github.jugbot.screen

import io.github.jugbot.Mod
import io.github.jugbot.blockentity.ShrineBlockEntity
import io.github.jugbot.entity.zone.ShrineZoneEntity
import net.minecraft.client.GameNarrator
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.MultiLineTextWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class ShrineScreen(blockEntity: ShrineBlockEntity) extends Screen(GameNarrator.NO_TITLE) {
  private var leftPos = 0
  private var topPos = 0

  private var materialList: MultiLineTextWidget = _

  private def shrineInfo =
    s"""Level: ${blockEntity.tier}
       |Blocks required within ${ShrineZoneEntity.DEFAULT_RADIUS} meters:
       |${blockEntity.nextTierRequirements.map(req => s"- ${req.toRenderableString}").mkString("\n")}
       |""".stripMargin.replaceAll("\r", "")

  override def init(): Unit = {
    super.init()
    this.leftPos = (this.width - ShrineScreen.imageWidth) / 2
    this.topPos = (this.height - ShrineScreen.imageHeight) / 2

    materialList = MultiLineTextWidget(leftPos + 8, topPos + 8, Component.literal(shrineInfo), this.font)
    materialList.setMaxWidth(ShrineScreen.imageWidth)
    materialList.setMaxRows(6)

    addRenderableWidget(materialList)
  }

  override def tick(): Unit = {
    super.tick()
    materialList.setMessage(Component.literal(shrineInfo))
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    this.renderBackground(guiGraphics)
    guiGraphics.blit(ShrineScreen.background,
                     this.leftPos,
                     this.topPos,
                     0,
                     0,
                     ShrineScreen.imageWidth,
                     ShrineScreen.imageHeight
    )
    super.render(guiGraphics, mouseX, mouseY, partialTicks)
  }
}

object ShrineScreen {
  private val imageWidth = 248
  private val imageHeight = 166
  private val background = new ResourceLocation(Mod.MOD_ID, "textures/gui/shrine.png")
}
