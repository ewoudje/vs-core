package org.valkyrienskies.core.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.imifou.jsonschema.module.addon.AddonModule
import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonModule
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.valkyrienskies.core.config.VSConfigClass.VSConfigClassSide.CLIENT
import org.valkyrienskies.core.config.VSConfigClass.VSConfigClassSide.COMMON
import org.valkyrienskies.core.config.VSConfigClass.VSConfigClassSide.SERVER
import org.valkyrienskies.core.networking.impl.PacketCommonConfigUpdate
import org.valkyrienskies.core.networking.impl.PacketServerConfigUpdate
import org.valkyrienskies.core.networking.simple.registerClientHandler
import org.valkyrienskies.core.networking.simple.registerServerHandler
import org.valkyrienskies.core.networking.simple.sendToServer
import org.valkyrienskies.core.util.serialization.VSJacksonUtil
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.reflect.full.memberProperties

data class VSConfigClass(
    val clazz: Class<*>,
    val name: String,
    val client: SidedVSConfigClass?,
    val common: SidedVSConfigClass?,
    val server: SidedVSConfigClass?
) {

    val sides = listOfNotNull(client, common, server)

    private fun makeServerConfigUpdatePacket(): PacketServerConfigUpdate? {
        if (server == null) return null
        return PacketServerConfigUpdate(server.clazz, mapper.valueToTree(server.inst))
    }

    private fun makeCommonConfigUpdatePacket(): PacketServerConfigUpdate? {
        if (common == null) return null
        return PacketServerConfigUpdate(common.clazz, mapper.valueToTree(common.inst))
    }

    companion object {
        internal val mapper = VSJacksonUtil.configMapper

        private val registeredConfigMap = HashMap<Class<*>, VSConfigClass>()

        private val JSON_SCHEMA_GENERATOR_VERSION = SchemaVersion.DRAFT_2019_09
        private val JSON_SCHEMA_VALIDATOR_VERSION = SpecVersion.VersionFlag.V201909

        private val schemaGeneratorConfig = SchemaGeneratorConfigBuilder(
            mapper,
            JSON_SCHEMA_GENERATOR_VERSION,
            OptionPreset.FULL_DOCUMENTATION
        )
            .with(AddonModule())
            .with(JacksonModule())
            .with(
                Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
                Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
                Option.NONPUBLIC_STATIC_FIELDS,
                Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT
            )
            .build()

        init {
            registerNetworkHandlers()
        }

        private fun UpdatableConfig.getSide(): VSConfigClassSide? =
            VSConfigClassSide.VALUES.find { it.name == this::class.java.name }

        fun UpdatableConfig.syncConfigToClient() {
            when (getSide()) {
                null -> {
                    val registeredConfig = getRegisteredConfig(this::class.java)

                    registeredConfig.makeCommonConfigUpdatePacket()?.sendToServer()
                }
                CLIENT -> {
                    throw IllegalArgumentException("Cannot sync client config to client")
                }
                SERVER -> {
                    throw IllegalArgumentException("Currently syncing server config to client is unsupported...")
                }
                COMMON -> {
                    val registeredConfig = getRegisteredConfig(this::class.java.enclosingClass)
                    registeredConfig.makeCommonConfigUpdatePacket()!!.sendToServer()
                }
            }
        }

        fun UpdatableConfig.syncConfigToServer() {
            when (getSide()) {
                null -> {
                    val registeredConfig = getRegisteredConfig(this::class.java)

                    registeredConfig.makeServerConfigUpdatePacket()?.sendToServer()
                    registeredConfig.makeCommonConfigUpdatePacket()?.sendToServer()
                }
                CLIENT -> {
                    throw IllegalArgumentException("Cannot sync client config to server")
                }
                SERVER -> {
                    val registeredConfig = getRegisteredConfig(this::class.java.enclosingClass)
                    registeredConfig.makeServerConfigUpdatePacket()!!.sendToServer()
                }
                COMMON -> {
                    val registeredConfig = getRegisteredConfig(this::class.java.enclosingClass)
                    registeredConfig.makeCommonConfigUpdatePacket()!!.sendToServer()
                }
            }
        }

        private fun getRegisteredConfig(mainClass: Class<*>): VSConfigClass {
            return requireNotNull(registeredConfigMap[mainClass]) {
                "This UpdatableConfig is not registered as a main config nor recognized as a subclass of a main class"
            }
        }

        private fun attemptUpdate(
            mainClass: Class<*>, newConfig: JsonNode, side: (VSConfigClass) -> SidedVSConfigClass?
        ) {
            try {
                val config = side(registeredConfigMap[mainClass]!!) ?: return
                mapper.readerForUpdating(config.inst).readValue(newConfig, config.clazz)
                config.onUpdate
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        private fun registerNetworkHandlers() {
            PacketServerConfigUpdate::class.registerServerHandler { (mainClass, newConfig), player ->
                if (player.isAdmin) {
                    attemptUpdate(mainClass, newConfig) { it.server }
                }
            }
            PacketServerConfigUpdate::class.registerClientHandler { (mainClass, newConfig) ->
                attemptUpdate(mainClass, newConfig) { it.server }
            }
            PacketCommonConfigUpdate::class.registerServerHandler { (mainClass, newConfig), player ->
                if (player.isAdmin) {
                    attemptUpdate(mainClass, newConfig) { it.common }
                }
            }
            PacketCommonConfigUpdate::class.registerClientHandler { (mainClass, newConfig) ->
                attemptUpdate(mainClass, newConfig) { it.common }
            }
        }

        private fun getSidedConfigClassAndInstance(side: VSConfigClassSide, mainClass: Class<*>): Pair<Class<*>, Any>? {
            return if (mainClass.kotlin.objectInstance != null) {
                require(mainClass.kotlin.memberProperties.isEmpty()) {
                    "Type A (Kotlin Object) main config class ($mainClass) may not have member properties"
                }

                val sidedConfig = mainClass.kotlin.nestedClasses.find { it.simpleName == side.subclassName }
                    ?: return null
                val instance = sidedConfig.objectInstance
                    ?: throw IllegalArgumentException(
                        "Type A (Kotlin Object) main config class ($mainClass) " +
                            "sided subclass ($sidedConfig) must be an object"
                    )

                Pair(sidedConfig.java, instance)
            } else {
                val sidedConfigField =
                    mainClass.fields.find { Modifier.isStatic(it.modifiers) && it.name == side.fieldName }
                        ?: return null

                val instance = sidedConfigField.get(null)
                    ?: throw IllegalArgumentException(
                        "Type B (Java Class) main config class ($mainClass) property $sidedConfigField is null!"
                    )

                Pair(sidedConfigField.type, instance)
            }
        }

        private fun createSidedVSConfigClass(
            parentName: String, side: VSConfigClassSide, mainClass: Class<*>
        ): SidedVSConfigClass? {
            val (clazz, inst) = getSidedConfigClassAndInstance(side, mainClass) ?: return null

            val generator = SchemaGenerator(schemaGeneratorConfig)

            val schemaJson = generator.generateSchema(clazz)
            (schemaJson.get("properties") as? ObjectNode)?.putObject("\$schema")?.put("type", "string")

            val schema = JsonSchemaFactory.getInstance(JSON_SCHEMA_VALIDATOR_VERSION).getSchema(schemaJson)

            val onUpdate = if (inst is UpdatableConfig) inst::onUpdate else ({})

            return SidedVSConfigClass(clazz, inst, side.subclassName, parentName, schemaJson, schema, onUpdate)
        }

        fun getOrRegisterConfig(name: String, clazz: Class<*>): VSConfigClass {
            val registered = registeredConfigMap[clazz]
            if (registered != null)
                return registered

            val client = createSidedVSConfigClass(name, CLIENT, clazz)
            val common = createSidedVSConfigClass(name, COMMON, clazz)
            val server = createSidedVSConfigClass(name, SERVER, clazz)

            val configClass = VSConfigClass(clazz, name, client, common, server)

            registeredConfigMap[clazz] = configClass

            return configClass
        }
    }

    private enum class VSConfigClassSide(
        val subclassName: String,
        val getSidedClass: (VSConfigClass) -> SidedVSConfigClass?,
        val fieldName: String = subclassName.uppercase()
    ) {
        CLIENT("Client", { it.client }),
        COMMON("Common", { it.common }),
        SERVER("Server", { it.server });

        companion object {
            val VALUES = values().toList()
        }
    }

    class SidedVSConfigClass(
        val clazz: Class<*>,
        val inst: Any,
        val sideName: String,
        val parentName: String,
        val schemaJson: JsonNode,
        val schema: JsonSchema,
        val onUpdate: () -> Unit
    ) {

        /**
         * @return an error message if the schema validation failed
         */
        fun createOrReadConfig(configDir: Path): String? {
            val mapper = VSConfigClass.mapper

            val name = "${parentName}_$sideName"
            val schemaRelativePath = "schemas/$name.schema.json"
            val schemaPath = configDir.resolve(schemaRelativePath)
            val configPath = configDir.resolve("$name.json")

            Files.createDirectories(configDir.resolve("schemas"))
            Files.deleteIfExists(schemaPath)
            mapper.writeValue(Files.newBufferedWriter(schemaPath, StandardOpenOption.CREATE_NEW), schemaJson)

            if (Files.exists(configPath)) {
                val json = mapper.readTree(Files.newBufferedReader(configPath))
                val errors = schema.validate(json)

                if (errors.isNotEmpty()) {
                    return errors.joinToString(separator = "\n")
                }

                mapper.readerForUpdating(inst).treeToValue(json, clazz)
            }

            val json = mapper.valueToTree<ObjectNode>(inst)
            json.put("\$schema", schemaRelativePath)
            mapper.writeValue(Files.newBufferedWriter(configPath, StandardOpenOption.CREATE_NEW), json)

            return null
        }
    }
}


