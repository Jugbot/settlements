package io.github.jugbot.entity

import com.mojang.brigadier.StringReader
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument
import net.minecraft.commands.arguments.item.ItemPredicateArgument
import net.minecraft.core.Vec3i
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.{Container, SimpleContainer}
import net.minecraft.world.entity.{EntityType, EquipmentSlot, Mob}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import io.github.jugbot.extension.Container.*
import net.minecraft.world.entity.player.Player


class InventoryMob(entityType: EntityType[FaeEntity], world: Level) extends Mob(entityType, world), Container {
  private val extraInventory = new SimpleContainer(8)

  protected def parseBlockPredicate(blockQuery: String) =
    BlockPredicateArgument(
      CommandBuildContext.simple(this.getServer.registryAccess(), this.getServer.getWorldData.enabledFeatures())
    ).parse(StringReader(blockQuery))

  protected def parseItemPredicate(itemQuery: String) = ItemPredicateArgument(
    CommandBuildContext.simple(this.getServer.registryAccess(), this.getServer.getWorldData.enabledFeatures())
  ).parse(StringReader(itemQuery))

  protected def items =
    extraInventory.items

  /**
   * Moves an item from the extra inventory to the equipment slot if possible.
   * @param itemQuery
   * @param slot
   * @return
   */
  protected def equipFromInventory(itemQuery: String, slot: EquipmentSlot) = {
    val itemTester = parseItemPredicate(itemQuery)
    val maybeItem = extraInventory.items.zipWithIndex.find((itemStack, _) => itemTester.test(itemStack))
    maybeItem match {
      case Some((itemStack, index)) =>
        extraInventory.setItem(index, getItemBySlot(slot))
        setItemSlot(slot, itemStack)
        true
      case None => false
    }
  }

  protected def query(items: Seq[ItemStack])(itemQuery: String) = {
    val itemTester = parseItemPredicate(itemQuery)
    items.zipWithIndex.filter((itemStack, _) => itemTester.test(itemStack))
  }

  protected def count(items: Seq[ItemStack])(itemQuery: String) =
    query(items)(itemQuery).map((i, _) => i).foldLeft(0)((acc, itemStack) => acc + itemStack.getCount)

  /**
   * Utility to transfer items from one container to another until the first container satisfies the amount.
   * @param from
   * @param to
   * @param itemQuery
   * @param amount
   * @return
   */
  protected def transferItemsUntil(from: Container, to: Container, itemQuery: String, amount: Int): Boolean = {
    val slotsWithItem = query(from.items)(itemQuery)
    val existingItemCount = count(from.items)(itemQuery)
    var remaining = existingItemCount - amount
    for {
      (itemStack, index) <- slotsWithItem
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

  override def canPickUpLoot = true

  override def canTakeItem(itemStack: ItemStack): Boolean =
    extraInventory.canAddItem(itemStack)

  override def getPickupReach: Vec3i = InventoryMob.ITEM_PICKUP_REACH

  override def pickUpItem(itemEntity: ItemEntity): Unit = super.pickUpItem(itemEntity)

  /**
   * Equip an item into equipment slots or extra inventory. Overrides default behavior which equips items into equipment slots.
   * @param itemStack The item to equip, is not mutated.
   * @return The itemStack that was equipped.
   */
  override def equipItemIfPossible(itemStack: ItemStack): ItemStack =
    val leftovers = extraInventory.addItem(itemStack)
    itemStack.copyWithCount(itemStack.getCount - leftovers.getCount)

  override def canReplaceCurrentItem(itemStack: ItemStack, itemStack2: ItemStack): Boolean = true

  def isEquipmentBetterThan(incoming: ItemStack, current: ItemStack): Boolean =
    super.canReplaceCurrentItem(incoming, current)

  // TODO: Accessing the equipment slots as part of inventory
  override def getContainerSize: Int =
    this.extraInventory.getContainerSize

  override def isEmpty: Boolean =
    this.extraInventory.isEmpty

  override def getItem(index: Int): ItemStack =
    this.extraInventory.getItem(index)

  override def removeItem(index: Int, amount: Int): ItemStack =
    this.extraInventory.removeItem(index, amount)

  override def removeItemNoUpdate(i: Int): ItemStack =
    this.extraInventory.removeItemNoUpdate(i)

  override def setItem(i: Int, itemStack: ItemStack): Unit =
    this.extraInventory.setItem(i, itemStack)

  override def setChanged(): Unit =
    this.extraInventory.setChanged()

  override def stillValid(player: Player): Boolean =
    this.extraInventory.stillValid(player)

  override def clearContent(): Unit =
    this.extraInventory.clearContent()
}

private object InventoryMob {
  private val ITEM_PICKUP_REACH = new Vec3i(2, 2, 2)
}
