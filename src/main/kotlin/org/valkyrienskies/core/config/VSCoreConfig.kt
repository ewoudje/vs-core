package org.valkyrienskies.core.config

import org.valkyrienskies.core.config.VSCoreConfig.Client.ShipMode.HELLO

object VSCoreConfig {

    @JvmField
    val CLIENT = Client()

    @JvmField
    val COMMON = Common()

    @JvmField
    val SERVER = Server()

    class Client {
        var testNum: Int = 0

        var mode: ShipMode = HELLO

        enum class ShipMode {
            HELLO, GOODBYE, MAYBE
        }

        val subcat = SubCategory()

        var list = listOf("apple", "banana", "pear")

        class SubCategory {
            var hi: String = "hello!"

            var subcat = SubCategory2()

            class SubCategory2 {
                var bye: String = "bye!"
            }
        }
    }

    class Common {

    }

    class Server {

    }
}

fun main() {
    val config = VSConfigClass.registerConfig("vs_core", VSCoreConfig::class.java)
    println(config.client!!.schemaJson.toPrettyString())
}
