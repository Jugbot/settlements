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

enum BlackboardKey {
  case bed_position
}

sealed trait FaeBehavior

object FaeBehavior {
  case class unknown() extends FaeBehavior
  case class unimplemented() extends FaeBehavior
  case class sleep() extends FaeBehavior
  case class is_tired() extends FaeBehavior
  case class has(value: BlackboardKey) extends FaeBehavior
  case class claim_bed() extends FaeBehavior
  case class bed_is_valid() extends FaeBehavior
  case class is_at_location(blockPos: BlackboardKey) extends FaeBehavior
  case class has_nav_path_to(blockPos: BlackboardKey) extends FaeBehavior
  case class create_nav_path_to(blockPos: BlackboardKey) extends FaeBehavior
  case class current_path_unobstructed() extends FaeBehavior
  case class move_along_current_path() extends FaeBehavior

  def valueOf(name: String, args: Map[String, BlackboardKey]): Option[FaeBehavior] =
    io.github.jugbot.meta.valueOf[FaeBehavior](name, args)
}

object FaeBehaviorTree {
  private var behaviorTreeMap: Map[String, Node[FaeBehavior]] = Map()
  val map: Map[String, Node[FaeBehavior]] = behaviorTreeMap

  private val extension_regex = """^.*\.json5?$""".r

  object Loader extends SimplePreparableReloadListener[Map[String, Node[FaeBehavior]]] {
    private def deserializeJson(reader: Reader): Node[FaeBehavior] = {
      val javaType: JavaType =
        BTMapper.mapper.getTypeFactory.constructType(classOf[FaeBehavior])

      val typeRef = BTMapper.mapper.getTypeFactory
        .constructSimpleType(classOf[Node[?]], Array(javaType))

      Try[Node[FaeBehavior]](BTMapper.mapper.readValue(reader, typeRef)) match {
        case Success(value) => value
        case Failure(exception) =>
          LOGGER.warn("Unable to parse custom behavior.")
          LOGGER.warn(exception)
          ActionNode(FaeBehavior.unknown())
      }
    }

    override def prepare(resourceManager: ResourceManager,
                         profilerFiller: ProfilerFiller
    ): Map[String, Node[FaeBehavior]] = {
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

    override def apply(map: Map[String, Node[FaeBehavior]],
                       resourceManager: ResourceManager,
                       profilerFiller: ProfilerFiller
    ): Unit =
      FaeBehaviorTree.behaviorTreeMap = map
  }
}
