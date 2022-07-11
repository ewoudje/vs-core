package org.valkyrienskies.core.api

import com.fasterxml.jackson.annotation.JsonIgnore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.valkyrienskies.core.VSRandomUtils
import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.game.ships.ShipObjectServer
import org.valkyrienskies.core.util.serialization.VSJacksonUtil

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

    @Test
    fun testAttachmentInterfaces() {
        val shipData = VSRandomUtils.randomShipData()
        val user = TestShipUser()

        shipData.saveAttachment(user)

        Assertions.assertEquals(user.ship, shipData)
        Assertions.assertEquals(user, shipData.getAttachment(TestShipUser::class.java))

        val shipDataSerialized = VSJacksonUtil.defaultMapper.writeValueAsBytes(shipData)
        val shipDataDeserialized = VSJacksonUtil.defaultMapper.readValue(shipDataSerialized, ShipData::class.java)

        Assertions.assertNotNull(shipData.getAttachment(TestShipUser::class.java))
        Assertions.assertEquals(shipData.getAttachment(TestShipUser::class.java)!!.ship, shipDataDeserialized)
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

internal class TestShipUser : ShipUser {
    @JsonIgnore
    override var ship: Ship? = null
}
