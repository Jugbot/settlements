package io.github.jugbot.entity

import java.util.function.Supplier
import com.google.common.base.Suppliers
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.Mob

class FaeEntity(entityType: EntityType[? <: Mob], world: Level)
    extends Mob(entityType, world) {
}

object FaeEntity {
  final val TYPE: Supplier[EntityType[FaeEntity]] = Suppliers.memoize(() =>
    EntityType.Builder
      .of[FaeEntity](new FaeEntity(_, _), MobCategory.MISC)
      .sized(0.98f, 0.7f)
      .build("fae_entity")
  );
}