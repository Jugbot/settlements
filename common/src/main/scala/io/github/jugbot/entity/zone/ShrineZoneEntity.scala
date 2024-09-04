package io.github.jugbot.entity.zone

import com.google.common.base.Suppliers
import io.github.jugbot.block.ShrineBlock
import io.github.jugbot.extension.AABB.toBoundingBox
import net.minecraft.world.entity.{EntityType, MobCategory}
import net.minecraft.world.level.Level

import java.util.function.Supplier

class ShrineZoneEntity(entityType: EntityType[ShrineZoneEntity], world: Level) extends ZoneEntity(entityType, world) {
  override def getZoneType: ZoneType = ZoneType.Structure

  override def tick(): Unit = {
    super.tick()
    val centerBlock = this.getBoundingBox.toBoundingBox.getCenter
    if !level.getBlockState(centerBlock).getBlock.isInstanceOf[ShrineBlock] then this.discard()
  }
}

object ShrineZoneEntity {
  val DEFAULT_RADIUS = 7

  final val TYPE: Supplier[EntityType[ShrineZoneEntity]] = Suppliers.memoize(() =>
    EntityType.Builder
      .of[ShrineZoneEntity](new ShrineZoneEntity(_, _), MobCategory.MISC)
      .build("shrine_zone")
  )
}
