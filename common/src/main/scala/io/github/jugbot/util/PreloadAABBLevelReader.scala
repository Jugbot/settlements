package io.github.jugbot.util

import net.minecraft.core.{BlockPos, Direction, Holder, RegistryAccess}
import net.minecraft.world.entity.Entity
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.biome.{Biome, BiomeManager}
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.border.WorldBorder
import net.minecraft.world.level.chunk.{ChunkAccess, ChunkStatus}
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.lighting.LevelLightEngine
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.VoxelShape

import java.util
import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Preloads an area of blocks into memory to avoid sporadic chunk loading/ cache misses.
 */
class PreloadAABBLevelReader(levelReader: LevelReader, aabb: AABB) extends LevelReader {
  private val blocks = BlockPos.betweenClosedStream(aabb).iterator().asScala
  private val zipped = blocks.map(blockPos => (blockPos.immutable(), levelReader.getBlockState(blockPos)))
  private val map = zipped.toMap

  override def getChunk(i: Int, j: Int, chunkStatus: ChunkStatus, bl: Boolean): ChunkAccess = ???

  override def hasChunk(i: Int, j: Int): Boolean = true

  override def getHeight(types: Heightmap.Types, i: Int, j: Int): Int = ???

  override def getSkyDarken: Int = ???

  override def getBiomeManager: BiomeManager = ???

  override def getUncachedNoiseBiome(i: Int, j: Int, k: Int): Holder[Biome] = ???

  override def isClientSide: Boolean = ???

  override def getSeaLevel: Int = ???

  override def dimensionType(): DimensionType = ???

  override def registryAccess(): RegistryAccess = ???

  override def enabledFeatures(): FeatureFlagSet = ???

  override def getShade(direction: Direction, bl: Boolean): Float = ???

  override def getLightEngine: LevelLightEngine = ???

  override def getWorldBorder: WorldBorder = ???

  override def getEntityCollisions(entity: Entity, aABB: AABB): util.List[VoxelShape] = ???

  override def getBlockEntity(blockPos: BlockPos): BlockEntity = null

  override def getBlockState(blockPos: BlockPos): BlockState = map.get(blockPos) match
    case Some(blockState) => blockState
    case None             => throw new NoSuchElementException(s"BlockState not found for position: $blockPos in $aabb")

  override def getFluidState(blockPos: BlockPos): FluidState = ???
}
