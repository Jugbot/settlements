package io.github.jugbot.entity

import io.github.jugbot.extension.Container.Query.*
import net.minecraft.core.Vec3i
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.npc.InventoryCarrier
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.entity.{EquipmentSlot, Mob}
import net.minecraft.world.inventory.{AbstractContainerMenu, ChestMenu, MenuType}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.*

trait ExtraInventory extends Mob, InventoryCarrier, MenuProvider {
  private val extraInventory = new SimpleContainer(9)

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit =
    super.addAdditionalSaveData(compoundTag)
    this.writeInventoryToTag(compoundTag)

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit =
    super.readAdditionalSaveData(compoundTag)
    this.readInventoryFromTag(compoundTag)

  /**
   * Moves an item from the extra inventory to the equipment slot if possible.
   * @param itemQuery
   * @param slot
   * @return
   */
  protected def equipFromInventory(itemQuery: String, slot: EquipmentSlot): Boolean = {
    val maybeItem = this.getInventory.query(itemQuery).headOption
    maybeItem match {
      case Some(ItemSlot(itemStack, index)) =>
        this.getInventory.setItem(index, getItemBySlot(slot))
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

  override def mobInteract(player: Player, interactionHand: InteractionHand): InteractionResult =
    player.openMenu(this)
    if !player.level.isClientSide then InteractionResult.CONSUME else InteractionResult.SUCCESS

  override def createMenu(i: SlotIndex, inventory: Inventory, player: Player): AbstractContainerMenu =
    new ChestMenu(MenuType.GENERIC_9x1, i, inventory, this.getInventory, 1);

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

  override def getInventory: SimpleContainer =
    this.extraInventory
}

private object ExtraInventory {
  private val ITEM_PICKUP_REACH = new Vec3i(2, 2, 2)
}
