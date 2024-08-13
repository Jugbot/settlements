package io.github.jugbot.extension

import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

object Container {
  extension (inventory: Container) {
    def items: IndexedSeq[ItemStack] = for {
      i <- 0 until inventory.getContainerSize
    } yield inventory.getItem(i)

    def size = items.length

    // Methods below were copied from SimpleStorage impl

    /**
     * Adds an item to the inventory. If the item is not fully added, the remaining item is returned.
     * @param itemStack The item to add, not mutated.
     * @return The remaining item
     */
    def addItem(itemStack: ItemStack): ItemStack = 
      if (itemStack.isEmpty) ItemStack.EMPTY
      else {
        val itemStack2 = itemStack.copy
        inventory.moveItemToOccupiedSlotsWithSameType(itemStack2)
        if (itemStack2.isEmpty) ItemStack.EMPTY
        else {
          inventory.moveItemToEmptySlots(itemStack2)
          if (itemStack2.isEmpty) ItemStack.EMPTY
          else itemStack2
        }
      }
    private def moveItemToEmptySlots(itemStack: ItemStack): Unit = {
      for (i <- 0 until inventory.size) {
        val itemStack2 = inventory.getItem(i)
        if (itemStack2.isEmpty) {
          inventory.setItem(i, itemStack.copyAndClear)
          return
        }
      }
    }
    private def moveItemToOccupiedSlotsWithSameType(itemStack: ItemStack): Unit = {
      for (i <- 0 until inventory.size) {
        val itemStack2 = inventory.getItem(i)
        if (ItemStack.isSameItemSameTags(itemStack2, itemStack)) {
          inventory.moveItemsBetweenStacks(itemStack, itemStack2)
          if (itemStack.isEmpty) return
        }
      }
    }
    private def moveItemsBetweenStacks(itemStack: ItemStack, itemStack2: ItemStack): Unit = {
      val i = Math.min(inventory.getMaxStackSize, itemStack2.getMaxStackSize)
      val j = Math.min(itemStack.getCount, i - itemStack2.getCount)
      if (j > 0) {
        itemStack2.grow(j)
        itemStack.shrink(j)
        inventory.setChanged()
      }
    }
  }
}