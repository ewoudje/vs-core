package org.valkyrienskies.core.game

import kotlin.random.Random

object VSOptions {
    var renderDebugText = true
    var renderShipCenterOfMass = true
    var udpPort = Random.nextInt(Short.MAX_VALUE.toInt()) // Server only
}
