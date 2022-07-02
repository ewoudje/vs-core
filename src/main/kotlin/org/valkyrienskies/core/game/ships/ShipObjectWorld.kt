package org.valkyrienskies.core.game.ships

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.plus
import org.valkyrienskies.core.game.DimensionId
import org.valkyrienskies.core.game.VSBlockType
import org.valkyrienskies.core.util.coroutines.TickableCoroutineDispatcher

/**
 * Manages all the [ShipObject]s in a world.
 */
abstract class ShipObjectWorld(
    open val queryableShipData: QueryableShipDataCommon,
) {

    abstract val shipObjects: Map<ShipId, ShipObject>

    private val _dispatcher = TickableCoroutineDispatcher()
    val dispatcher: CoroutineDispatcher = _dispatcher
    val coroutineScope = MainScope() + _dispatcher

    var tickNumber = 0
        private set

    protected open fun tickShips() {
        try {
            _dispatcher.tick()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        tickNumber++
    }

    open fun onSetBlock(
        posX: Int,
        posY: Int,
        posZ: Int,
        dimensionId: DimensionId,
        oldBlockType: VSBlockType,
        newBlockType: VSBlockType,
        oldBlockMass: Double,
        newBlockMass: Double
    ) {
        // If there is a ShipData at this position and dimension, then tell it about the block update
        queryableShipData.getShipDataFromChunkPos(posX shr 4, posZ shr 4, dimensionId)
            ?.onSetBlock(posX, posY, posZ, oldBlockType, newBlockType, oldBlockMass, newBlockMass)
    }

    abstract fun destroyWorld()
}
