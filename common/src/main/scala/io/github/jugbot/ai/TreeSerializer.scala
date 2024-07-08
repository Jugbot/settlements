package io.github.jugbot.ai

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonToken, StreamReadFeature, Version}
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.{
  BeanProperty,
  DeserializationContext,
  DeserializationFeature,
  JavaType,
  JsonDeserializer,
  SerializationFeature,
  SerializerProvider
}
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, EnumModule}

import java.util
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava}

type T = Any

class NodeDeserializer extends StdDeserializer[Node](classOf[Node]) with ContextualDeserializer {

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

  private def expectToken(p: JsonParser, ctxt: DeserializationContext, token: JsonToken): Unit =
    if p.getCurrentToken != token then {
      throw ctxt.reportBadDefinition(classOf[Node], f"expected $token")
    }

  private def deserializeStringPart(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node = {
    val action = genericDeserializer.deserialize(p, ctxt)
    // TODO
    ActionNode(???, Map.empty)
  }

  private def deserializeSeqPart(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Seq[Node] = {
    expectToken(p, ctxt, JsonToken.START_ARRAY)
    p.nextToken()
    var children = Seq[Node]()
    while p.currentToken() != JsonToken.END_ARRAY do children :+= deserializeObjPart(p, ctxt)
    p.nextToken()
    children
  }

  private def deserializeObjPart(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node = {
    expectToken(p, ctxt, JsonToken.START_OBJECT)
    expectToken(p, ctxt, JsonToken.FIELD_NAME)
    val nodeType = p.currentName()
    val node: Node = nodeType match {
      case "selector" =>
        p.nextToken()
        val children = deserializeSeqPart(p, ctxt)
        SelectorNode(children*)
      case "sequence" =>
        p.nextToken()
        val children = deserializeSeqPart(p, ctxt)
        SequenceNode(children*)
      case action =>
        val args = this._(p, ctxt)
        ActionNode(action, args)

    }
    expectToken(p, ctxt, JsonToken.END_OBJECT)
    p.nextToken()
    node
  }

  private def deserializeAnyPart(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node | Seq[Node] =
    p.getCurrentToken match {
      case JsonToken.START_OBJECT => deserializeObjPart(p, ctxt)
      case JsonToken.START_ARRAY  => deserializeSeqPart(p, ctxt)
      case JsonToken.VALUE_STRING => deserializeStringPart(p, ctxt)
    }

  override def deserialize(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node =
    deserializeAnyPart(p, ctxt) match {
      case (value: Node) => value
      case Seq(_) =>
        ctxt.reportBadDefinition(classOf[Seq[Node]],
                                 "Cannot start a file with an array, either object or string is allowed."
        )
    }

  override def createContextual(
    ctxt: DeserializationContext,
    property: BeanProperty
  ): JsonDeserializer[Node] = {
    val contextualType: JavaType = ctxt.getContextualType

    val typeParameters: List[JavaType] =
      List.from(contextualType.getBindings.getTypeParameters.asScala)
    if typeParameters.size != 1 then {
      throw new IllegalStateException("size should be 1")
    }

    val genericType: JavaType = typeParameters.head

    val genericDeserializer: JsonDeserializer[Object] =
      ctxt.findContextualValueDeserializer(genericType, property)
    NodeDeserializer(genericType, genericDeserializer)
  }
}

object NodeSerializer extends StdSerializer[Node](classOf[Node]) {
  override def serialize(
    value: Node,
    gen: JsonGenerator,
    provider: SerializerProvider
  ): Unit = {
    gen.writeStartObject()
    value match {
      case ActionNode(action, args) =>
        provider.defaultSerializeField(action, args, gen);
      case SelectorNode(children*) =>
        provider.defaultSerializeField("selector", children, gen);
      case SequenceNode(children*) =>
        provider.defaultSerializeField("sequence", children, gen);
    }
    gen.writeEndObject()
  }
}

object BTModule
    extends SimpleModule(
      "BTModule",
      new Version(1, 0, 0, null, "io.github.jugbot", "settlements"),
      Map(
        classOf[Node] -> new NodeDeserializer()
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
    .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
    .build()
}
