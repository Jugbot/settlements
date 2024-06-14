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

type T = Any

class NodeDeserializer extends StdDeserializer[Node[T]](classOf[Node[T]]) with ContextualDeserializer {

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

  private def deserializeSeqPart(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Seq[Node[T]] = {
    if p.getCurrentToken != JsonToken.START_ARRAY then {
      throw ctxt.reportBadDefinition(classOf[Node[T]], "expected START_OBJECT")
    }
    p.nextToken()
    var children = Seq[Node[Any]]()
    while p.currentToken() != JsonToken.END_ARRAY do children :+= deserializeObjPart(p, ctxt)
    p.nextToken()
    children
  }

  private def deserializeObjPart(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node[T] = {
    if p.getCurrentToken != JsonToken.START_OBJECT then {
      throw ctxt.reportBadDefinition(classOf[Node[T]], "expected START_OBJECT")
    }
    if p.nextToken() != JsonToken.FIELD_NAME then {
      throw ctxt.reportBadDefinition(classOf[Node[T]], "expected FIELD_NAME")
    }
    val nodeType = p.currentName()
    val node = nodeType match {
      case "action" =>
        p.nextToken()
        val action = genericDeserializer.deserialize(p, ctxt)
        p.nextToken()
        ActionNode[T](action)
      case "selector" =>
        p.nextToken()
        val children = deserializeSeqPart(p, ctxt)
        SelectorNode[T](children*)
      case "sequence" =>
        p.nextToken()
        val children = deserializeSeqPart(p, ctxt)
        SequenceNode[T](children*)
      case _ =>
        throw ctxt.weirdKeyException(
          classOf[Node[T]],
          nodeType,
          f"Unexpected key name $nodeType"
        )
    }
    if p.currentToken() != JsonToken.END_OBJECT then {
      throw ctxt.reportBadDefinition(classOf[Node[T]], "expected END_OBJECT")
    }
    p.nextToken()
    node
  }

  override def deserialize(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Node[T] =
    deserializeObjPart(p, ctxt)

  override def createContextual(
    ctxt: DeserializationContext,
    property: BeanProperty
  ): JsonDeserializer[Node[T]] = {
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

object NodeSerializer extends StdSerializer[Node[?]](classOf[Node[?]]) {
  override def serialize(
    value: Node[?],
    gen: JsonGenerator,
    provider: SerializerProvider
  ): Unit = {
    gen.writeStartObject()
    value match {
      case ActionNode(action) =>
        provider.defaultSerializeField("action", action, gen);
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
        classOf[Node[Any]] -> new NodeDeserializer()
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
