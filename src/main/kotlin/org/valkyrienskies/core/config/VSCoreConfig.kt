package org.valkyrienskies.core.config

object VSCoreConfig {

    object Client {
        @JvmStatic
        var renderDebugText = true
    }

    object Common {

    }

    object Server {

    }
}

// fun main() {
//     val config = SidedVSConfigClass.registerConfig(VSCoreConfig::class.java)
//     val path = Paths.get("configs")
//     config.generateConfigFiles(VSCoreConfig, "vs_core_config", path)
// }
