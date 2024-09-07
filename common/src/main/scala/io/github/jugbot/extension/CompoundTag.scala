package io.github.jugbot.extension

import net.minecraft.nbt.{CompoundTag, ListTag, NbtUtils, Tag}
import net.minecraft.world.entity.Entity

import java.util.UUID
import scala.jdk.CollectionConverters.IteratorHasAsScala

object CompoundTag {
  extension (compoundTag: CompoundTag)
    def putUUIDS(key: String, values: Iterable[UUID]): Unit =
      val tags = ListTag()
      values.map(NbtUtils.createUUID).foreach(tag => tags.addTag(0, tag))
      compoundTag.put(key, tags)
    def getUUIDS(key: String): Iterable[UUID] =
      compoundTag.getList(key, Tag.TAG_INT_ARRAY).iterator().asScala.map(NbtUtils.loadUUID).toList

    def putEntities(key: String, values: Iterable[Entity]): Unit =
      putUUIDS(key, values.map(_.getUUID))
    def getEntities[E <: Entity](key: String, getter: Function[UUID, Option[Entity]]): Iterable[E] =
      getUUIDS(key).flatMap(getter).map(_.asInstanceOf[E])
}
