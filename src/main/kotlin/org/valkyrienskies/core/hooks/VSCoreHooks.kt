package org.valkyrienskies.core.hooks

import org.valkyrienskies.core.hooks.PlayState.CLIENT_TITLESCREEN
import org.valkyrienskies.core.hooks.PlayState.SERVERSIDE
import java.nio.file.Path
import kotlin.properties.Delegates

object VSCoreHooks {

    @JvmStatic
    var isPhysicalClient by Delegates.notNull<Boolean>()

    @JvmStatic
    lateinit var configDir: Path

    lateinit var playState: PlayState

    fun init() {
        playState = if (isPhysicalClient) CLIENT_TITLESCREEN else SERVERSIDE
    }
}
