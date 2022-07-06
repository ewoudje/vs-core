package org.valkyrienskies.core.config

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema
import java.nio.file.Paths

object VSCoreConfig {
    @JsonPropertyDescription("Test description")
    @JsonSchema(maxLength = 12)
    var testValueOne = "test"
}

fun main() {
    val config = VSConfigClass.createConfig(VSCoreConfig::class.java)
    config.generateConfigFiles(VSCoreConfig, "vs_core_config", Paths.get("configs"))
}
