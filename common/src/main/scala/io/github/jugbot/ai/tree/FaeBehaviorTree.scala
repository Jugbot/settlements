package io.github.jugbot.ai.tree

import com.fasterxml.jackson.databind.JavaType
import io.github.jugbot.Mod.LOGGER
import io.github.jugbot.ai.{
  isValidTree,
  ActionNode,
  BTMapper,
  BehaviorFailure,
  BehaviorRunning,
  BehaviorStatus,
  BehaviorSuccess,
  Node,
  ParameterizedNode,
  SelectorNode,
  SequenceNode
}
import net.minecraft.server.packs.resources.{ResourceManager, SimplePreparableReloadListener}
import net.minecraft.util.profiling.ProfilerFiller

import java.io.Reader
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}
import scala.collection.mutable.Set as MutableSet
import scala.collection.mutable.Seq as MutableSeq
import io.github.jugbot.entity.FaeEntity
import net.minecraft.server.packs.resources.Resource
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import io.github.jugbot.meta

object FaeBehaviorTree {
  private var behaviorTreeMap: Map[String, ParameterizedNode] = Map()
  def map: Map[String, ParameterizedNode] = behaviorTreeMap
  val fallback: ParameterizedNode = ActionNode("unknown", Map.empty)

  private val extension_regex = """^.*\.json5?$""".r

  object Loader extends SimplePreparableReloadListener[Map[String, ParameterizedNode]] {
    private def deserializeJson(reader: Reader, location: ResourceLocation): ParameterizedNode =
      Try[ParameterizedNode](BTMapper.mapper.readValue(reader, classOf[ParameterizedNode])) match {
        case Success(value) => value
        case Failure(exception) =>
          LOGGER.warn(f"Unable to parse custom behavior for '${location.getNamespace}:${location.getPath}'")
          LOGGER.warn(exception)
          fallback
      }

    override def prepare(resourceManager: ResourceManager,
                         profilerFiller: ProfilerFiller
    ): Map[String, ParameterizedNode] = {
      val resourceMap = resourceManager
        .listResources("behavior/fae", location => extension_regex.matches(location.getPath))
        .asScala
      val btMap = resourceMap.map { case (key, value) =>
        val fileName = Path.of(key.getPath).getFileName.toString
        val name = fileName.substring(0, fileName.lastIndexOf('.'))
        val btNode = deserializeJson(value.openAsReader, key)
        (name, btNode)
      }
      btMap.toMap
    }

    override def apply(map: Map[String, ParameterizedNode],
                       resourceManager: ResourceManager,
                       profilerFiller: ProfilerFiller
    ): Unit =
      assert(isValidTree(map, meta.valueOf[Behavior[FaeEntity, Blackboard]]), "Could not validate behavior configs!")
      FaeBehaviorTree.behaviorTreeMap = map
  }
}
