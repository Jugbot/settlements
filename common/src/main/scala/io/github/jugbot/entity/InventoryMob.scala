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

import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsScala

//class DelegatedSeq[A] extends mutable.Seq[A] {
//
//  override def update(idx: Int, elem: A): Unit = ???
//
//  override def apply(i: Int): A = ???
//
//  override def length: Int = ???
//
//  override def iterator: Iterator[A] = ???
//}

class InventoryMob(entityType: EntityType[FaeEntity], world: Level) extends Mob(entityType, world), Container {
  private val extraInventory = new SimpleContainer(8)

  protected def parseBlockPredicate(blockQuery: String) =
    BlockPredicateArgument(
      CommandBuildContext.simple(this.getServer.registryAccess(), this.getServer.getWorldData.enabledFeatures())
    ).parse(StringReader(blockQuery))

  protected def parseItemPredicate(itemQuery: String) = ItemPredicateArgument(
    CommandBuildContext.simple(this.getServer.registryAccess(), this.getServer.getWorldData.enabledFeatures())
  ).parse(StringReader(itemQuery))

  protected def items = {
    extraInventory.items ++ this.getHandSlots.asScala ++ this.getArmorSlots.asScala
  }

  protected def equipFromInventory(itemQuery: String, slot: EquipmentSlot) = {
    val itemTester = parseItemPredicate((itemQuery))
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
    val itemTester = parseItemPredicate((itemQuery))
    items.zipWithIndex.filter((itemStack, _) => itemTester.test(itemStack))
  }

  protected def count(items: Seq[ItemStack])(itemQuery: String) =
    query(items)(itemQuery).map((i, _) => i).foldLeft(0)((acc, itemStack) => acc + itemStack.getCount)

  protected def transferItemsUntil(from: Container, to: Container, itemQuery: String, amount: Int): Boolean = {
    val slotsWithItem = query(from.items)(itemQuery)
    val currentItemCount = count(to.items)(itemQuery)
    var remaining = amount - currentItemCount
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
    super.canTakeItem(itemStack) || extraInventory.canAddItem(itemStack)

  override def getPickupReach: Vec3i = InventoryMob.ITEM_PICKUP_REACH

  override def pickUpItem(itemEntity: ItemEntity): Unit = super.pickUpItem(itemEntity)

  /**
   * Equip an item into equipment slots or extra inventory.
   * @param itemStack The item to equip, is not mutated.
   * @return The itemStack that was equipped.
   */
  override def equipItemIfPossible(itemStack: ItemStack): ItemStack =
    val result = super.equipItemIfPossible(itemStack)
    val leftover = itemStack.getCount - result.getCount
    if leftover > 0 then
      val leftovers = extraInventory.addItem(itemStack.copyWithCount(leftover))
      result.grow(leftover - leftovers.getCount)
    result

  override def canReplaceCurrentItem(itemStack: ItemStack, itemStack2: ItemStack): Boolean = true

  def isEquipmentBetterThan(incoming: ItemStack, current: ItemStack): Boolean = super.canReplaceCurrentItem(incoming, current)

  override def getContainerSize: Int = this.items.length

  override def isEmpty: Boolean = this.items.forall(_.isEmpty)

  override def getItem(index: Int): ItemStack = this.items(index)

  // Remove item using index associated with items def
  override def removeItem(index: Int, amount: Int): ItemStack =
    var mutableIndex = index
    if mutableIndex < extraInventory.getContainerSize then
      return extraInventory.removeItem(index, amount)
    mutableIndex -= extraInventory.getContainerSize
    if mutableIndex < List(getHandSlots).length then
      val existing = getItem(index)
      setItemSlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.HAND, mutableIndex), ItemStack.EMPTY)
      return existing
    mutableIndex -= List(getHandSlots).length
    if mutableIndex < List(getArmorSlots).length then
      val existing = getItem(index)
      setItemSlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, mutableIndex), ItemStack.EMPTY)
      return existing
    ItemStack.EMPTY

  override def removeItemNoUpdate(i: Int): ItemStack = ???

  override def setItem(i: Int, itemStack: ItemStack): Unit = ???

  override def setChanged(): Unit = ???

  override def stillValid(player: Player): Boolean = ???

  override def clearContent(): Unit = ???
}

private object InventoryMob {
  private val ITEM_PICKUP_REACH = new Vec3i(1, 0, 1)
}