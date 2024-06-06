package io.github.jugbot.entity

import java.util.function.Supplier
import com.google.common.base.Suppliers
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import java.lang
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import java.{util => ju}
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LightningBolt
import net.minecraft.server.level.ServerLevel

class FaeEntity(entityType: EntityType[? <: LivingEntity], world: Level)
    extends LivingEntity(entityType, world) {

  override def getArmorSlots(): java.lang.Iterable[ItemStack] = ju.List.of()

  override def getItemBySlot(equipmentSlot: EquipmentSlot): ItemStack =
    ItemStack.EMPTY

  override def setItemSlot(
      equipmentSlot: EquipmentSlot,
      itemStack: ItemStack
  ): Unit = {}

  override def getMainArm(): HumanoidArm = HumanoidArm.RIGHT

  override def isPushable(): Boolean = false

  override def doPush(entity: Entity) = {}

  override def attackable(): Boolean = false

  override def isEffectiveAi(): Boolean = false
}

object FaeEntity {
  final val TYPE: Supplier[EntityType[FaeEntity]] = Suppliers.memoize(() =>
    EntityType.Builder
      .of[FaeEntity](new FaeEntity(_, _), MobCategory.MISC)
      .sized(0.98f, 0.7f)
      .build("fae_entity")
  );

  def createAttributes(): AttributeSupplier.Builder = {
    LivingEntity
      .createLivingAttributes()
      .add(Attributes.MAX_HEALTH, 10.0)
      .add(Attributes.MOVEMENT_SPEED, 0.2f);
  }
}
