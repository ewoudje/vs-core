package org.valkyrienskies.core.config

object VSCoreConfig {

    @JvmField
    val SERVER = Server()

    class Server {
        var udpPort = 25565
    }
}

fun main() {
    val config = VSConfigClass.registerConfig("vs_core", VSCoreConfig::class.java)
    println(config.client!!.schemaJson.toPrettyString())
}
