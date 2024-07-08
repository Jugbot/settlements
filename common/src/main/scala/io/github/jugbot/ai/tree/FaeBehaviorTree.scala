package io.github.jugbot.ai.tree

import com.fasterxml.jackson.databind.JavaType
import io.github.jugbot.Mod.LOGGER
import io.github.jugbot.ai.{ActionNode, BTMapper, BehaviorTree, Node, SelectorNode, SequenceNode, Status}
import net.minecraft.server.packs.resources.{ResourceManager, SimplePreparableReloadListener}
import net.minecraft.util.profiling.ProfilerFiller

import java.io.Reader
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

enum BlackboardKey {
  case bed_position
}

enum FaeBehavior {
  case unknown
  case unimplemented
  case tree
  case sleep
  case is_tired
  case has
  case claim_bed
  case bed_is_valid
  case is_at_location
  case has_nav_path_to
  case create_nav_path_to
  case current_path_unobstructed
  case move_along_current_path
}

// object FaeBehavior {
//   def valueOf(jsonValue: String): FaeBehavior = {
//     val tokens = jsonValue.split("""[(),]""").toList

//     // TODO: This could probably be done automatically using reflection or macros
//     tokens match {
//       case behaviorType :: args =>
//         val keys = args.map(BlackboardKey.valueOf)
//         behaviorType match {
//           case "unimplemented" => FaeBehavior.unimplemented
//           case _               => FaeBehavior.unknown
//         }
//       case _ => FaeBehavior.unknown
//     }
//   }
// }

object FaeBehaviorTree {
//  private val goToBlock: Node = SelectorNode(
//    ActionNode(FaeBehavior.is_at_location("arg1")),
//    SequenceNode(
//      SelectorNode(ActionNode(FaeBehavior.has_nav_path_to("arg1")), ActionNode(FaeBehavior.create_nav_path_to(key))),
//      ActionNode(FaeBehavior.current_path_unobstructed),
//      ActionNode(FaeBehavior.move_along_current_path)
//    )
//  )
//
//  private val claimBed = ActionNode(FaeBehavior.claim_bed)
//  private val sleep = SequenceNode(
//    ActionNode(FaeBehavior.is_tired),
//    SelectorNode(
//      SequenceNode(
//        ActionNode(FaeBehavior.has(BlackboardKey.bed_position)),
//        ActionNode(FaeBehavior.bed_is_valid)
//      ),
//      claimBed
//    ),
//    Reference("goToBlock",
//              Map(
//                "arg1" -> BlackboardKey.bed_position
//              )
//    ),
//    ActionNode(FaeBehavior.sleep)
//  )
//  private val survival = SequenceNode(sleep)
//  val root: Node = SequenceNode(survival)
//  val evaluated = run(
//    root,
//    (a: FaeBehavior, ctxt: Map[String, BlackboardKey]) =>
//      a match {
//        case FaeBehavior.has(key) =>
//          val bbk = ctxt.get(key)
//          Success
//        case _ => Failure
//      },
//    Map(
//      "goToBlock" -> goToBlock
//    )
//  )

  private var behaviorTree = Option.empty[BehaviorTree]
  def state(perform: (String, Map[String, String]) => Status): Status =
    behaviorTree match {
      case Some(bt) => bt.state(perform)
      case None     => io.github.jugbot.ai.Failure
    }

  private val extension_regex = """^.*\.json5?$""".r

  object Loader extends SimplePreparableReloadListener[Map[String, Node]] {
    private def deserializeJson(reader: Reader): Node = {
      val javaType: JavaType =
        BTMapper.mapper.getTypeFactory.constructType(classOf[FaeBehavior])

      val typeRef = BTMapper.mapper.getTypeFactory
        .constructSimpleType(classOf[Node], Array(javaType))

      Try[Node](BTMapper.mapper.readValue(reader, typeRef)) match {
        case Success(value) => value
        case Failure(exception) =>
          LOGGER.warn("Unable to parse custom behavior.")
          LOGGER.warn(exception)
          ActionNode("unknown")
      }
    }

    override def prepare(resourceManager: ResourceManager, profilerFiller: ProfilerFiller): Map[String, Node] = {
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

    override def apply(map: Map[String, Node], resourceManager: ResourceManager, profilerFiller: ProfilerFiller): Unit =
      FaeBehaviorTree.behaviorTree = Option(BehaviorTree(map))
  }
}
