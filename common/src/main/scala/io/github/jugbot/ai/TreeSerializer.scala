package io.github.jugbot.ai

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.EnumModule

import java.util
import java.util.Arrays
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.jdk.CollectionConverters.MapHasAsJava
import com.fasterxml.jackson.core.JsonGenerator.Feature
import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.`type`.TypeFactory
import scala.collection.mutable

type T = Any

class NodeDeserializer extends StdDeserializer[Node[T]](classOf[Node[T]]) {

  private var genericType: JavaType = _
  private var genericDeserializer: JsonDeserializer[Object] = _

  def this(
    genericType: JavaType,
    genericDeserializer: JsonDeserializer[Object]
  ) = {
    this()
    this.genericType = genericType
    this.genericDeserializer = genericDeserializer
  }

  private def deserializeSeq[T](
    p: JsonParser,
    ctxt: DeserializationContext,
    injest: (p: JsonParser, ctxt: DeserializationContext) => T
  ): Seq[T] = {
    if p.getCurrentToken != JsonToken.START_ARRAY then {
      throw ctxt.reportBadDefinition(classOf[Node[T]], "expected START_OBJECT")
    }
    p.nextToken()
    var children = Seq[T]()
    while p.currentToken() != JsonToken.END_ARRAY do children :+= injest(p, ctxt)
    p.nextToken()
    children
  }

  private def deserializeObj[T](
    p: JsonParser,
    ctxt: DeserializationContext,
    injest: (keyName: String, p: JsonParser, ctxt: DeserializationContext) => T
  ): Map[String, T] = {
    if p.getCurrentToken != JsonToken.START_OBJECT then {
      throw ctxt.reportBadDefinition(classOf[Node[T]], "expected START_OBJECT")
    }
    p.nextToken()
    var result = Map[String, T]()
    while p.currentToken() == JsonToken.FIELD_NAME do {
      val nodeType = p.currentName()
      p.nextToken()
      result += ((nodeType, injest(nodeType, p, ctxt)))
    }
    if p.currentToken() != JsonToken.END_OBJECT then {
      throw ctxt.reportBadDefinition(classOf[Node[T]], "expected END_OBJECT")
    }
    p.nextToken()
    result
  }

  private def deserializeNodeString(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node[T] = {
    val raw = p.getValueAsString()
    if raw == null then {
      throw ctxt.reportBadDefinition(classOf[Node[T]], f"could not read string at ${p.currentLocation}")
    }
    p.nextToken()

    val (Array(behaviorType), args) = raw.split("""[(),]""").splitAt(1)

    val argMap = args.map { s =>
      val splitIndex = s.indexOf("=")
      if splitIndex == -1 then
        ctxt.reportBadDefinition(
          classOf[String],
          f"Expected 'behaviorName(argName=argValue)' form in behavior definition at ${p.currentLocation}"
        )
      (s.take(splitIndex), s.drop(splitIndex + 1))
    }.toMap

    ActionNode(behaviorType, argMap)
  }

  private def deserializeNodeEntry(
    nodeType: String,
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node[T] =
    nodeType match {
      case "selector" =>
        val children = deserializeSeq(p, ctxt, deserializeNode)
        SelectorNode[T](children*)
      case "sequence" =>
        val children = deserializeSeq(p, ctxt, deserializeNode)
        SequenceNode[T](children*)
      case "condition" =>
        val args = deserializeObj(p, ctxt, (_, _p, _ctxt) => deserializeNode(_p, _ctxt))
        IfElseNode[T](args("if"), args("then"), args("else"))
      case action =>
        val args = deserializeObj(p,
                                  ctxt,
                                  (_, p, _) => {
                                    val value = p.getValueAsString()
                                    p.nextToken()
                                    value
                                  }
        )

        ActionNode[T](action, args)
    }

  private def deserializeNode(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node[T] =
    if p.isExpectedStartObjectToken() then {
      val m = deserializeObj(p, ctxt, deserializeNodeEntry(_, _, _))
      m.head(1)
    } else {
      deserializeNodeString(p, ctxt)
    }

  override def deserialize(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node[T] =
    deserializeNode(p, ctxt)
}

object NodeSerializer extends StdSerializer[ParameterizedNode](classOf[ParameterizedNode]) {
  override def serialize(
    value: ParameterizedNode,
    gen: JsonGenerator,
    provider: SerializerProvider
  ): Unit = {
    def serializeSingleFieldObject(name: String, value: Object): Unit = {
      gen.writeStartObject()
      provider.defaultSerializeField(name, value, gen)
      gen.writeEndObject()
    }

    def serializeActionShorthand(name: String, params: Map[String, String]): Unit = {
      val strNamedArgs = params.map((key, value) => f"$key=$value").mkString(",")
      val strFunction = f"$name($strNamedArgs)"
      val shorthand = if strNamedArgs.nonEmpty then strFunction else name
      provider.defaultSerializeValue(shorthand, gen)
    }

    value match {
      case ActionNode(action, params) =>
        serializeActionShorthand(action, params);
      case SelectorNode(children*) =>
        serializeSingleFieldObject("selector", children);
      case SequenceNode(children*) =>
        serializeSingleFieldObject("sequence", children);
      case _ => ???
    }
  }
}

object BTModule
    extends SimpleModule(
      "BTModule",
      new Version(1, 0, 0, null, "io.github.jugbot", "settlements"),
      Map(
        classOf[Node[?]] -> new NodeDeserializer()
      ).asJava,
      util.Arrays.asList(NodeSerializer)
    )

object BTMapper {
  val mapper: JsonMapper = JsonMapper
    .builder()
    .addModule(DefaultScalaModule)
    .addModule(EnumModule)
    .addModule(BTModule)
    .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
    .enable(SerializationFeature.INDENT_OUTPUT)
    .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
    .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
    .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
    .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
    .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
    .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
    .disable(JsonWriteFeature.QUOTE_FIELD_NAMES)
    .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
    .build()
}
