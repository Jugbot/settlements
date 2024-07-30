package io.github.jugbot.screen

import io.github.jugbot.Mod
import io.github.jugbot.blockentity.ShrineBlockEntity
import net.minecraft.client.GameNarrator
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.AnvilMenu

class ShrineScreen(blockEntity: ShrineBlockEntity) extends Screen(GameNarrator.NO_TITLE) {
  private val leftPos = (this.width - ShrineScreen.imageWidth) / 2
  private val topPos = (this.height - ShrineScreen.imageHeight) / 2

  override def init(): Unit = {
    super.init()
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    this.renderBackground(guiGraphics)
    guiGraphics.blit(ShrineScreen.background, this.leftPos + 59, this.topPos + 20, 0, ShrineScreen.imageHeight, 110, 16)
    super.render(guiGraphics, mouseX, mouseY, partialTicks)
  }
}

object ShrineScreen {
  private val imageWidth = 176
  private val imageHeight = 166
  private val background = new ResourceLocation("textures/gui/container/anvil.png");
}
