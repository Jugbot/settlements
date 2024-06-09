package io.github.jugbot.ai

import java.util.Arrays
import java.util.ArrayList
import com.google.common.collect.Lists
import com.fasterxml.jackson.module.scala.EnumModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.core.`type`.WritableTypeId
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonFormat.Feature

object NodeSerializer extends StdSerializer[Node[_]](classOf[Node[_]]) {
  override def serialize(
      value: Node[_],
      gen: JsonGenerator,
      provider: SerializerProvider
  ): Unit = {
    gen.writeStartObject()
    value match {
      case ActionNode(action) =>
        provider.defaultSerializeField("action", (action), gen);
      case SelectorNode(children @ _*) =>
        provider.defaultSerializeField("selector", (children), gen);
      case SequenceNode(children @ _*) =>
        provider.defaultSerializeField("sequence", (children), gen);
    }
    gen.writeEndObject()
  }
  override def serializeWithType(
      value: Node[_],
      gen: JsonGenerator,
      serializers: SerializerProvider,
      typeSer: TypeSerializer
  ): Unit = {
    val typeId: WritableTypeId = typeSer.typeId(value, JsonToken.START_OBJECT);

    typeSer.writeTypePrefix(gen, typeId)
    serialize(value, gen, serializers);
    typeSer.writeTypeSuffix(gen, typeId)
  }
}

object BTModule
    extends SimpleModule(
      "BTModule",
      new Version(1, 0, 0, null, "io.github.jugbot", "settlements"),
      null,
      Arrays.asList(NodeSerializer)
    )

object BTMapper {

  val mapper = JsonMapper
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
    .build()
}
