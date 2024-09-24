package io.github.jugbot.entity

import com.google.common.collect.Lists
import dev.architectury.extensions.network.EntitySpawnExtension
import dev.architectury.networking.NetworkManager
import io.github.jugbot.Mod.LOGGER
import io.github.jugbot.util.TagType
import net.minecraft.nbt.{CompoundTag, ListTag}
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.world.entity.{Entity, EntityType}

import scala.jdk.CollectionConverters.{CollectionHasAsScala, SetHasAsJava}
import scala.jdk.OptionConverters.RichOptional

/**
 * A utility for holding references to other entities like passengers & vehicles but without the extra baggage.
 *
 * @see PersistentEntitySectionManager.class
 */

trait WithChildren[Self <: WithChildren[Self, Child], Child <: WithParent[Child, Self]](nbtTagName: String)
    extends Entity
    with EntitySpawnExtension { self: Self =>
  var children: Set[Child] = Set.empty

  def addChild(child: Child): Unit = {
    children += child
    child.parent = Some(this)
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
      compoundTag.put(nbtTagName, listTag)
    }
  }

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit =
    if compoundTag.contains(nbtTagName, TagType.LIST) then {
      val listTag = compoundTag.getList(nbtTagName, TagType.COMPOUND)
      for i <- 0 until listTag.size do {
        val entity = EntityType.create(listTag.getCompound(i), level).toScala
        entity match {
          case Some(child) =>
            level.addFreshEntity(child)
            addChild(child.asInstanceOf[Child])
          case _ => LOGGER.warn(s"Unable to load child entity from NBT, returned $entity")
        }
      }
    }

  /**
   * Spawn data for the client
   */

  override def getAddEntityPacket: Packet[ClientGamePacketListener] = NetworkManager.createAddEntityPacket(this)

  def saveAdditionalSpawnData(buf: FriendlyByteBuf): Unit =
    buf.writeCollection[Child](children.asJava, (buf, child) => buf.writeInt(child.getId))

  def loadAdditionalSpawnData(buf: FriendlyByteBuf): Unit = {
    val mutSet = buf
      .readCollection(
        Lists.newArrayListWithCapacity[Child],
        buf => {
          val id = buf.readInt()
          level.getEntity(id) match {
            case child => child.asInstanceOf[Child]
            case null =>
              LOGGER.error(s"Unable to load child entity with id $id")
              null.asInstanceOf[Child]
          }
        }
      )
      .asScala
      .filter(_ != null)
    children = mutSet.toSet
  }
}

trait WithParent[Self <: WithParent[Self, Parent], Parent <: WithChildren[Parent, Self]] extends Entity { self: Self =>
  var parent: Option[Parent] = None

  def addParent(parent: Parent): Unit = {
    this.parent = Some(parent)
    parent.children += (this)
  }

  override def shouldBeSaved(): Boolean =
    parent.isEmpty && super.shouldBeSaved()

  override def save(compoundTag: CompoundTag): Boolean =
    parent.isEmpty && super.save(compoundTag)
}

/**
 * A utility for holding references to other entities like passengers & vehicles but without the extra baggage.
 * TODO: [HARD] Reduce duplication with WithChildren and WithParent
 */
trait EntityHeirarchy[Self <: EntityHeirarchy[Self]](nbtTagName: String) extends Entity with EntitySpawnExtension {
  self: Self =>

  private var children: Set[Self] = Set.empty
  private var parent: Option[Self] = None

  def getChildren = children
  def getParent = parent

  def addChild(child: Self): Unit = {
    this.children += child
    child.parent = Some(this)
  }

  def removeChild(child: Self): Unit = {
    this.children -= child
    child.parent = None
  }

  def setParent(parent: Self): Unit =
    parent.addChild(this)

  def removeParent(): Unit =
    parent.foreach(_.removeChild(this))

  def removeChildren(): Unit =
    for { child <- this.children }
      this.removeChild(child)

  def addChildren(children: Set[Self]): Unit =
    for { child <- children }
      this.addChild(child)

  def setChildren(children: Set[Self]): Unit = {
    this.removeChildren()
    this.addChildren(children)
  }

  override def shouldBeSaved(): Boolean =
    parent.isEmpty && super.shouldBeSaved()

  override def save(compoundTag: CompoundTag): Boolean =
    parent.isEmpty && super.save(compoundTag)

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    val listTag = new ListTag

    for { child <- children } {
      val compoundTag2: CompoundTag = new CompoundTag
      if child.saveAsPassenger(compoundTag2) then {
        listTag.add(compoundTag2)
      }
    }

    if !listTag.isEmpty then {
      compoundTag.put(nbtTagName, listTag)
    }
  }

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit =
    if compoundTag.contains(nbtTagName, TagType.LIST) then {
      val listTag = compoundTag.getList(nbtTagName, TagType.COMPOUND)
      for i <- 0 until listTag.size do {
        val entity = EntityType.create(listTag.getCompound(i), level).toScala
        entity match {
          case Some(child) =>
            level.addFreshEntity(child)
            addChild(child.asInstanceOf[Self])
          case _ => LOGGER.warn(s"Unable to load child entity from NBT, returned $entity")
        }
      }
    }

  /**
   * Spawn data for the client
   */

  override def getAddEntityPacket: Packet[ClientGamePacketListener] = NetworkManager.createAddEntityPacket(this)

  def saveAdditionalSpawnData(buf: FriendlyByteBuf): Unit =
    buf.writeCollection[Self](children.asJava, (buf, child) => buf.writeInt(child.getId))

  def loadAdditionalSpawnData(buf: FriendlyByteBuf): Unit = {
    val mutSet = buf
      .readCollection(
        Lists.newArrayListWithCapacity[Self],
        buf => {
          val id = buf.readInt()
          level.getEntity(id) match {
            case child => child.asInstanceOf[Self]
            case null =>
              LOGGER.error(s"Unable to load child entity with id $id")
              null.asInstanceOf[Self]
          }
        }
      )
      .asScala
      .filter(_ != null)
    children = mutSet.toSet
  }
}
