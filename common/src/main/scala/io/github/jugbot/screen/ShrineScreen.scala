package io.github.jugbot.screen

import io.github.jugbot.Mod
import io.github.jugbot.blockentity.ShrineBlockEntity
import net.minecraft.client.GameNarrator
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

class ShrineScreen(blockEntity: ShrineBlockEntity) extends Screen(GameNarrator.NO_TITLE) {
  private var leftPos = 0
  private var topPos = 0

  override def init(): Unit = {
    super.init()
    this.leftPos = (this.width - ShrineScreen.imageWidth) / 2
    this.topPos = (this.height - ShrineScreen.imageHeight) / 2
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    this.renderBackground(guiGraphics)
    guiGraphics.blit(ShrineScreen.background, this.leftPos, this.topPos, 0, 0, ShrineScreen.imageWidth, ShrineScreen.imageHeight)
    super.render(guiGraphics, mouseX, mouseY, partialTicks)
  }
}

object ShrineScreen {
  private val imageWidth = 248
  private val imageHeight = 166
  private val background = new ResourceLocation(Mod.MOD_ID,"textures/gui/shrine.png");
}
