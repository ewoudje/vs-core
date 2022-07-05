package org.valkyrienskies.core.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.valkyrienskies.core.VSRandomUtils
import org.valkyrienskies.core.game.ships.ShipObjectServer

// Yes its a very simple test, but if somebody ever dares to break it we will know
internal class ShipApiTest {

    @Test
    fun testShipDataAbstraction() {
        val shipData = VSRandomUtils.randomShipData()

        abstractShipSaver(shipData)
        abstractShipUser(shipData, false)
    }

    @Test
    fun testShipObjectAbstraction() {
        val shipObject = ShipObjectServer(VSRandomUtils.randomShipData())

        abstractShipSaver(shipObject)
        abstractShipUser(shipObject, true)
    }

    fun abstractShipSaver(ship: Ship) {
        ship.saveAttachment<Int>(5)
        ship.setAttachment<Float>(3f)
    }

    fun abstractShipUser(ship: Ship, checkFloat: Boolean) {
        Assertions.assertEquals(ship.getAttachment<Int>(), 5)
        if (checkFloat) Assertions.assertEquals(ship.getAttachment<Float>(), 3f)
    }
}
