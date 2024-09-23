package io.github.jugbot.entity

import io.github.jugbot.Mod.LOGGER
import io.github.jugbot.util.TagType
import net.minecraft.nbt.{CompoundTag, ListTag}
import net.minecraft.world.entity.{Entity, EntityType}

import scala.jdk.OptionConverters.RichOptional

/**
 * A utility for holding references to other entities like passengers & vehicles but without the extra baggage.
 *
 * @see PersistentEntitySectionManager.class
 */

trait Owning[P <: Owning[P, C], C <: OwnedBy[P, C]](val childTag: String) extends Entity { self: P =>
  var children: Set[C] = Set.empty

  def addChild(child: C): Unit = {
    children += child
    child.parent = Some(self)
  }

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    val listTag = new ListTag

    for { child <- children } {
      val compoundTag2: CompoundTag = new CompoundTag
      if child.saveAsPassenger(compoundTag2) then {
        listTag.add(compoundTag2)
      }
    }

    if !listTag.isEmpty then {
      compoundTag.put(childTag, listTag)
    }
  }

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit =
    if compoundTag.contains(childTag, TagType.LIST) then {
      val listTag = compoundTag.getList(childTag, TagType.COMPOUND)
      for i <- 0 until listTag.size do {
        val entity = EntityType.create(listTag.getCompound(i), level).toScala
        entity match {
          case Some(child) => addChild(child.asInstanceOf[C])
          case _           => LOGGER.warn(s"Unable to load child entity from NBT, returned $entity")
        }
      }
    }
}

trait OwnedBy[P <: Owning[P, C], C <: OwnedBy[P, C]] extends Entity { self: C =>
  var parent: Option[P] = None

  override def shouldBeSaved(): Boolean =
    parent.isEmpty && super.shouldBeSaved()

  override def save(compoundTag: CompoundTag): Boolean =
    parent.isEmpty && super.save(compoundTag)
}
