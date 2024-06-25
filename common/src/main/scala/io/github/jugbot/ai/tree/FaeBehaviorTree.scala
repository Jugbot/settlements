package io.github.jugbot.ai.tree

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.Module
import io.github.jugbot.ai.{ActionNode, BTMapper, Node}
import net.minecraft.server.packs.resources.{ResourceManager, SimplePreparableReloadListener}
import net.minecraft.util.profiling.ProfilerFiller
import io.github.jugbot.Mod.LOGGER
import java.io.Reader
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

enum BlackboardKey {
  case bed_position
}

enum FaeBehavior(args: BlackboardKey*) {
  case unknown
  case unimplemented
  case tree(name: String)
  case sleep
  case is_tired
  case has(key: BlackboardKey) extends FaeBehavior(key)
  case claim_bed
  case bed_is_valid
  case is_at_location(destinationKey: BlackboardKey) extends FaeBehavior(destinationKey)
  case has_nav_path_to(destinationKey: BlackboardKey) extends FaeBehavior(destinationKey)
  case create_nav_path_to(destinationKey: BlackboardKey) extends FaeBehavior(destinationKey)
  case current_path_unobstructed
  case move_along_current_path
}

object FaeBehavior {
  def valueOf(jsonValue: String): FaeBehavior = {
    val tokens = jsonValue.split("""[(),]""").toList

    // TODO: This could probably be done automatically using reflection or macros
    tokens match {
      case behaviorType :: args =>
        val keys = args.map(BlackboardKey.valueOf)
        behaviorType match {
          case "unimplemented"           => FaeBehavior.unimplemented
          case "has" if keys.length >= 1 => FaeBehavior.has(keys(0))
          case _                         => FaeBehavior.unknown
        }
      case _ => FaeBehavior.unknown
    }
  }
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
          ActionNode(FaeBehavior.unknown)
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
