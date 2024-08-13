package io.github.jugbot.extension

import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

import scala.util.boundary

object Container {
  extension (inventory: Container) {
    def items: IndexedSeq[ItemStack] = for {
      i <- 0 until inventory.getContainerSize
    } yield inventory.getItem(i)

    // Methods below were copied from SimpleStorage impl

    /**
     * Adds an item to the inventory. If the item is not fully added, the remaining item is returned.
     * @param itemStack The item to add, not mutated.
     * @return The remaining item
     */
    def addItem(itemStack: ItemStack): ItemStack =
      if itemStack.isEmpty then ItemStack.EMPTY
      else {
        val itemStack2 = itemStack.copy
        inventory.moveItemToOccupiedSlotsWithSameType(itemStack2)
        if itemStack2.isEmpty then ItemStack.EMPTY
        else {
          inventory.moveItemToEmptySlots(itemStack2)
          if itemStack2.isEmpty then ItemStack.EMPTY
          else itemStack2
        }
      }
    private def moveItemToEmptySlots(itemStack: ItemStack): Unit =
      boundary:
        for i <- 0 until inventory.getContainerSize do {
          val itemStack2 = inventory.getItem(i)
          if itemStack2.isEmpty then {
            inventory.setItem(i, itemStack.copyAndClear)
            boundary.break()
          }
        }
    private def moveItemToOccupiedSlotsWithSameType(itemStack: ItemStack): Unit =
      boundary:
        for i <- 0 until inventory.getContainerSize do {
          val itemStack2 = inventory.getItem(i)
          if ItemStack.isSameItemSameTags(itemStack2, itemStack) then {
            inventory.moveItemsBetweenStacks(itemStack, itemStack2)
            if itemStack.isEmpty then boundary.break()
          }
        }
    private def moveItemsBetweenStacks(itemStack: ItemStack, itemStack2: ItemStack): Unit = {
      val i = Math.min(inventory.getMaxStackSize, itemStack2.getMaxStackSize)
      val j = Math.min(itemStack.getCount, i - itemStack2.getCount)
      if j > 0 then {
        itemStack2.grow(j)
        itemStack.shrink(j)
        inventory.setChanged()
      }
    }
  }
}
