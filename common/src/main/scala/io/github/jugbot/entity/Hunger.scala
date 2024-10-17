package io.github.jugbot.entity

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.Mob
import net.minecraft.world.item.{Item, ItemStack}
import net.minecraft.world.level.GameRules

/**
 * Adapted from net.minecraft.world.food.FoodData
 */
class FoodData {
  private var foodLevel = 20
  private var saturationLevel = 5.0f
  private var exhaustionLevel = .0f
  private var tickTimer = 0
  private var lastFoodLevel = 20

  private def eat(i: Int, f: Float): Unit = {
    this.foodLevel = Math.min(i + this.foodLevel, 20)
    this.saturationLevel = Math.min(this.saturationLevel + i.toFloat * f * 2.0f, this.foodLevel.toFloat)
  }

  def eat(itemStack: ItemStack): ItemStack =
    val item = itemStack.getItem
    if item.isEdible && !itemStack.isEmpty then {
      val foodProperties = item.getFoodProperties
      this.eat(foodProperties.getNutrition, foodProperties.getSaturationModifier)
      val newStack = itemStack.copyWithCount(itemStack.getCount - 1)
      if newStack.isEmpty then ItemStack.EMPTY
      else newStack
    } else itemStack

  def tick(entity: Hunger): Unit = {
    val difficulty = entity.level.getDifficulty
    this.lastFoodLevel = this.foodLevel
    if this.exhaustionLevel > 4.0f then {
      this.exhaustionLevel -= 4.0f
      if this.saturationLevel > 0.0f then this.saturationLevel = Math.max(this.saturationLevel - 1.0f, 0.0f)
      else this.foodLevel = Math.max(this.foodLevel - 1, 0)
    }
    val bl = entity.level.getGameRules.getBoolean(GameRules.RULE_NATURAL_REGENERATION)
    if bl && this.saturationLevel > 0.0f && entity.isHurt && this.foodLevel >= 20 then {
      this.tickTimer += 1
      if this.tickTimer >= 10 then {
        val f = Math.min(this.saturationLevel, 6.0f)
        entity.heal(f / 6.0f)
        this.addExhaustion(f)
        this.tickTimer = 0
      }
    } else if bl && this.foodLevel >= 18 && entity.isHurt then {
      this.tickTimer += 1
      if this.tickTimer >= 80 then {
        entity.heal(1.0f)
        this.addExhaustion(6.0f)
        this.tickTimer = 0
      }
    } else if this.foodLevel <= 0 then {
      this.tickTimer += 1
      if this.tickTimer >= 80 then {
        if entity.getHealth > 10.0f || (difficulty eq Difficulty.HARD) || entity.getHealth > 1.0f && (difficulty eq Difficulty.NORMAL)
        then entity.hurt(entity.damageSources.starve, 1.0f)
        this.tickTimer = 0
      }
    } else this.tickTimer = 0
  }

  def readAdditionalSaveData(compoundTag: CompoundTag): Unit =
    if compoundTag.contains("foodLevel", 99) then {
      this.foodLevel = compoundTag.getInt("foodLevel")
      this.tickTimer = compoundTag.getInt("foodTickTimer")
      this.saturationLevel = compoundTag.getFloat("foodSaturationLevel")
      this.exhaustionLevel = compoundTag.getFloat("foodExhaustionLevel")
    }

  def addAdditionalSaveData(compoundTag: CompoundTag): Unit = {
    compoundTag.putInt("foodLevel", this.foodLevel)
    compoundTag.putInt("foodTickTimer", this.tickTimer)
    compoundTag.putFloat("foodSaturationLevel", this.saturationLevel)
    compoundTag.putFloat("foodExhaustionLevel", this.exhaustionLevel)
  }

  def getFoodLevel: Int = this.foodLevel

  def getLastFoodLevel: Int = this.lastFoodLevel

  def needsFood: Boolean = this.foodLevel < 20

  def addExhaustion(f: Float): Unit =
    this.exhaustionLevel = Math.min(this.exhaustionLevel + f, 40.0f)

  def getExhaustionLevel: Float = this.exhaustionLevel

  def getSaturationLevel: Float = this.saturationLevel

  def setFoodLevel(i: Int): Unit =
    this.foodLevel = i

  def setSaturation(f: Float): Unit =
    this.saturationLevel = f

  def setExhaustion(f: Float): Unit =
    this.exhaustionLevel = f
}

trait Hunger extends Mob {
  val foodData = new FoodData

  def isHurt: Boolean = this.getHealth > 0.0f && this.getHealth < this.getMaxHealth

  override def tick(): Unit = {
    super.tick()
    foodData.tick(this)
  }

  override def addAdditionalSaveData(compoundTag: CompoundTag): Unit =
    super.addAdditionalSaveData(compoundTag)
    foodData.addAdditionalSaveData(compoundTag)

  override def readAdditionalSaveData(compoundTag: CompoundTag): Unit =
    super.readAdditionalSaveData(compoundTag)
    foodData.readAdditionalSaveData(compoundTag)
}
