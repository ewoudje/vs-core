package org.valkyrienskies.core.util.serialization

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.game.ships.ShipDataCommon
import java.io.InputStream

object VSJacksonUtil {
    /**
     * the default mapper for the standard Valkyrien Skies configuration for serializing
     * things, particularly [org.valkyrienskies.core.game.ShipData]
     */
    val defaultMapper = CBORMapper()

    /**
     * the mapper for Valkyrien Skies network transmissions (e.g., it ignores
     * [org.valkyrienskies.core.util.serialization.PacketIgnore] annotated fields
     */
    val packetMapper = CBORMapper()

    /**
     * the mapper for serializing delta updates to ShipData
     * It ignores [org.valkyrienskies.core.util.serialization.DeltaIgnore]
     */
    val deltaMapper = CBORMapper()

    /**
     * the mapper for configuration data
     */
    val configMapper = ObjectMapper()

    init {
        // Configure the mappers
        configureMapper(defaultMapper)
        configurePacketMapper(packetMapper)
        configureDeltaMapper(deltaMapper)
        configureConfigMapper(configMapper)
    }

    @JsonSerialize(`as` = ShipDataCommon::class)
    private object ShipDataServerMixin

    private fun configureConfigMapper(mapper: ObjectMapper) {
        mapper.enable(INDENT_OUTPUT)
        configureMapper(mapper)
    }

    private fun configurePacketMapper(mapper: ObjectMapper) {
        configureMapper(mapper)
        mapper.insertAnnotationIntrospector(IgnoringAnnotationIntrospector(PacketIgnore::class.java))
        mapper.addMixIn(ShipData::class.java, ShipDataServerMixin::class.java)
    }

    private fun configureDeltaMapper(mapper: ObjectMapper) {
        configureMapper(mapper)
        mapper.insertAnnotationIntrospector(
            IgnoringAnnotationIntrospector(PacketIgnore::class.java, DeltaIgnore::class.java)
        )
        mapper.addMixIn(ShipData::class.java, ShipDataServerMixin::class.java)
    }

    /**
     * Configures the selected object mapper to use the standard Valkyrien Skies configuration for
     * serializing things, particularly [org.valkyrienskies.core.game.ShipData]
     *
     * @param mapper The ObjectMapper to configure
     */
    private fun configureMapper(mapper: ObjectMapper) {
        mapper
            .registerModule(JOMLSerializationModule())
            .registerModule(VSSerializationModule())
            .registerModule(GuaveSerializationModule())
            .setVisibility(
                mapper.visibilityChecker
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            )
        // Serialize Kotlin data types
        mapper.registerKotlinModule()
    }
}

inline fun <reified T> ObjectMapper.readValue(buf: ByteBuf): T {
    return readValue(ByteBufInputStream(buf) as InputStream)
}

fun <T> ObjectMapper.readValue(buf: ByteBuf, clazz: Class<T>): T {
    return readValue(ByteBufInputStream(buf) as InputStream, clazz)
}

inline fun <reified A, reified B : A> SimpleModule.addAbstractTypeMapping(): SimpleModule =
    addAbstractTypeMapping(A::class.java, B::class.java)

inline fun <reified A, reified B> SimpleModule.setMixInAnnotation(): SimpleModule =
    setMixInAnnotation(A::class.java, B::class.java)

fun ObjectMapper.insertAnnotationIntrospector(ai: AnnotationIntrospector) {
    this.registerModule(object : SimpleModule() {
        override fun setupModule(context: SetupContext) {
            context.insertAnnotationIntrospector(ai)
            super.setupModule(context)
        }
    })
}

fun ObjectMapper.appendAnnotationIntrospector(ai: AnnotationIntrospector) {
    this.registerModule(object : SimpleModule() {
        override fun setupModule(context: SetupContext) {
            context.appendAnnotationIntrospector(ai)
            super.setupModule(context)
        }
    })
}
