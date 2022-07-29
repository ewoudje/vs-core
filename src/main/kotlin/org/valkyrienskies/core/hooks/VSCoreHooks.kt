package org.valkyrienskies.core.hooks

import org.valkyrienskies.core.config.VSConfigClass
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.game.ships.ShipObjectClientWorld
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld
import java.nio.file.Path

abstract class AbstractCoreHooks {
    abstract val isPhysicalClient: Boolean
    abstract val configDir: Path
    abstract val playState: PlayState

    abstract val currentShipServerWorld: ShipObjectServerWorld?
    abstract val currentShipClientWorld: ShipObjectClientWorld

    fun afterClientJoinServer(player: IPlayer) {
        VSConfigClass.afterClientJoinServer(player)
    }
}

lateinit var CoreHooks: AbstractCoreHooks
