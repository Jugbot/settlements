package io.github.jugbot.ai.tree

import com.fasterxml.jackson.databind.JavaType
import io.github.jugbot.Mod.LOGGER
import io.github.jugbot.ai.{ActionNode, BTMapper, Node}
import net.minecraft.server.packs.resources.{ResourceManager, SimplePreparableReloadListener}
import net.minecraft.util.profiling.ProfilerFiller

import java.io.Reader
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}
import io.github.jugbot.ai.ParameterizedNode

object BlackboardKey {
  val bed_position = "bed_position"
}

sealed trait FaeBehavior

object FaeBehavior {
  case class unknown() extends FaeBehavior
  case class unimplemented() extends FaeBehavior
  case class sleep() extends FaeBehavior
  case class is_tired() extends FaeBehavior
  case class has(value: String) extends FaeBehavior
  case class claim_bed() extends FaeBehavior
  case class bed_is_valid() extends FaeBehavior
  case class is_at_location(blockPos: String) extends FaeBehavior
  case class has_nav_path_to(blockPos: String) extends FaeBehavior
  case class create_nav_path_to(blockPos: String) extends FaeBehavior
  case class current_path_unobstructed() extends FaeBehavior
  case class move_along_current_path() extends FaeBehavior

  def valueOf(name: String, args: Map[String, String]): Option[FaeBehavior] =
    io.github.jugbot.meta.valueOf[FaeBehavior](name, args)
}

object FaeBehaviorTree {
  private var behaviorTreeMap: Map[String, ParameterizedNode] = Map()
  def map: Map[String, ParameterizedNode] = behaviorTreeMap
  val fallback: ParameterizedNode = ActionNode("unknown", Map.empty)

  private val extension_regex = """^.*\.json5?$""".r

  object Loader extends SimplePreparableReloadListener[Map[String, ParameterizedNode]] {
    private def deserializeJson(reader: Reader): ParameterizedNode =
      Try[ParameterizedNode](BTMapper.mapper.readValue(reader, classOf[ParameterizedNode])) match {
        case Success(value) => value
        case Failure(exception) =>
          LOGGER.warn("Unable to parse custom behavior.")
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
        val btNode = deserializeJson(value.openAsReader)
        (name, btNode)
      }
      btMap.toMap
    }

    override def apply(map: Map[String, ParameterizedNode],
                       resourceManager: ResourceManager,
                       profilerFiller: ProfilerFiller
    ): Unit =
      FaeBehaviorTree.behaviorTreeMap = map
  }
}
