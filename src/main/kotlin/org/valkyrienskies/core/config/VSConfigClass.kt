package org.valkyrienskies.core.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.imifou.jsonschema.module.addon.AddonModule
import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonModule
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

data class VSConfigClass<T>(
    val clazz: Class<T>,
    val schemaJson: JsonNode,
    val schema: JsonSchema
) {

    fun generateConfigFiles(inst: T, name: String, configDir: Path) {
        val schemaRelativePath = "schemas/$name.schema.json"
        val schemaPath = configDir.resolve(schemaRelativePath)
        val configPath = configDir.resolve("$name.json")

        val json = mapper.valueToTree<ObjectNode>(inst)
        json.put("\$schema", schemaRelativePath)

        Files.createDirectories(configDir.resolve("schemas"))

        println(schemaPath)
        Files.newBufferedWriter(configPath, StandardOpenOption.CREATE).use { writer ->
            mapper.writeValue(writer, json)
        }

        Files.newBufferedWriter(schemaPath, StandardOpenOption.CREATE).use { writer ->
            mapper.writeValue(writer, schemaJson)
        }
    }

    companion object {
        private val mapper = ObjectMapper()

        fun <T> createConfig(clazz: Class<T>): VSConfigClass<T> {
            val config = SchemaGeneratorConfigBuilder(
                mapper,
                SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON
            )
                .with(AddonModule())
                .with(JacksonModule())
                .with(
                    NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS, Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
                    Option.NONPUBLIC_STATIC_FIELDS
                )
                .build()

            val generator = SchemaGenerator(config)
            val schemaJson = generator.generateSchema(clazz)

            println(schemaJson)
            val schema = JsonSchemaFactory.byDefault().getJsonSchema(schemaJson)

            return VSConfigClass(clazz, schemaJson, schema)
        }
    }
}

