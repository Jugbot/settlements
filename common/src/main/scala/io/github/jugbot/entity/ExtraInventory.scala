package io.github.jugbot.entity

import io.github.jugbot.extension.Container.Query.*
import net.minecraft.core.Vec3i
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.{EquipmentSlot, Mob}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.{Container, Containers, SimpleContainer}

trait ExtraInventory extends Mob, Container {
  private val extraInventory = new SimpleContainer(8)

  /**
   * Moves an item from the extra inventory to the equipment slot if possible.
   * @param itemQuery
   * @param slot
   * @return
   */
  protected def equipFromInventory(itemQuery: String, slot: EquipmentSlot) = {
    val maybeItem = this.query(itemQuery).headOption
    maybeItem match {
      case Some((itemStack, index)) =>
        setItem(index, getItemBySlot(slot))
        setItemSlot(slot, itemStack)
        true
      case None => false
    }
  }

  override def dropAllDeathLoot(damageSource: DamageSource): Unit =
    Containers.dropContents(level, this, this.extraInventory)
    this.extraInventory.removeAllItems()
    super.dropAllDeathLoot(damageSource)

  override def canPickUpLoot = true

  override def canTakeItem(itemStack: ItemStack): Boolean =
    extraInventory.canAddItem(itemStack)

  override def getPickupReach: Vec3i = ExtraInventory.ITEM_PICKUP_REACH

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

private object ExtraInventory {
  private val ITEM_PICKUP_REACH = new Vec3i(2, 2, 2)
}
