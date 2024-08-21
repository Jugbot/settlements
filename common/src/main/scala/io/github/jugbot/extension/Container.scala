package io.github.jugbot.extension

import io.github.jugbot.util.itemPredicate
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

import scala.util.boundary

object Container {
  extension (inventory: Container) {
    def items: Seq[ItemStack] = for {
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

  object Query {
    type SlotIndex = Int
    case class ItemSlot(itemStack: ItemStack, slot: SlotIndex)

    extension (inventory: Container) {
      def query(itemQuery: String): Seq[ItemSlot] = {
        val predicate = itemPredicate(itemQuery)
        inventory.items.zipWithIndex.filter((itemStack, _) => predicate(itemStack)).map(ItemSlot(_, _))
      }

      def find(predicate: Function[ItemSlot, Boolean]): Option[ItemSlot] =
        inventory.items.zipWithIndex.map(ItemSlot(_, _)).find(predicate)

      def queryFirst(itemQuery: String): Option[ItemSlot] = {
        val predicate = itemPredicate(itemQuery)
        find((itemSlot: ItemSlot) => predicate(itemSlot.itemStack))
      }

      def count(itemQuery: String): Int =
        query(itemQuery).map(_.itemStack).foldLeft(0)((acc, itemStack) => acc + itemStack.getCount)

      /**
       * Utility to transfer items from one container to another.
       *
       * @param from      The container to transfer items from.
       * @param to        The container to transfer items to.
       * @param itemQuery The query for items that will be transferred.
       * @param amount    The amount of items to be transferred.
       * @return
       */
      private def transferItemsUntil(from: Container, to: Container, itemQuery: String, amount: Int): Boolean = {
        val slotsWithItem = from.query(itemQuery)
        var remaining = amount
        for {
          ItemSlot(itemStack, index) <- slotsWithItem
          if remaining > 0
        } {
          val toTransfer = Math.min(Math.max(0, remaining), itemStack.getCount)
          val newStack = itemStack.copy()
          newStack.setCount(toTransfer)
          val remainingStack = to.addItem(newStack)
          from.setItem(index, remainingStack)
          val transferredAmount = toTransfer - remainingStack.getCount
          remaining -= transferredAmount
        }
        remaining <= 0
      }

      def transferItemsUntilTargetHas(target: Container, itemQuery: String, amount: Int): Boolean =
        val existingItemCount = target.count(itemQuery)
        transferItemsUntil(inventory, target, itemQuery, amount - existingItemCount)

      /**
       * Transfer items from this inventory to another container until the queried items are at or below the amount.
       * @param container The container to transfer items to.
       * @param itemQuery The query to filter items in this inventory.
       * @param amount The amount of items in the container that will cease transfer.
       * @return
       */
      def transferItemsUntilSelfHas(target: Container, itemQuery: String, amount: Int): Boolean =
        val existingItemCount = inventory.count(itemQuery)
        transferItemsUntil(inventory, target, itemQuery, existingItemCount - amount)
    }
  }
}
