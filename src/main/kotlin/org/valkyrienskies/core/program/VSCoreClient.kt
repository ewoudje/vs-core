package org.valkyrienskies.core.program

import dagger.Component
import javax.inject.Inject

/**
 * An object that lives the entirety of the program.
 * Intended to be bound to MinecraftClient
 */
class VSCoreClient @Inject constructor() : VSCore() {

    @Component
    interface Factory {
        fun create(): VSCoreClient
    }
}
