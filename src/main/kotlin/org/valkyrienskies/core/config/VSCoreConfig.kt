package org.valkyrienskies.core.config

import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema
import org.valkyrienskies.core.config.VSCoreConfig.Client.ShipMode.HELLO

object VSCoreConfig {

    @JvmField
    val CLIENT = Client()

    @JvmField
    val COMMON = Common()

    @JvmField
    val SERVER = Server()

    class Client {
        @JsonSchema(title = "Render Debug Text", description = "Renders the VS2 debug HUD with TPS")
        var renderDebugText = true

        var testNum: Int = 0

        var mode: ShipMode = HELLO

        enum class ShipMode {
            HELLO, GOODBYE, MAYBE
        }
    }

    class Common {

    }

    class Server {

    }
}

fun main() {
    val config = VSConfigClass.getOrRegisterConfig("vs_core", VSCoreConfig::class.java)
    println(config.client!!.schemaJson.toPrettyString())
}
