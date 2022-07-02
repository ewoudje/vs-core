package org.valkyrienskies.core.networking

fun interface RegisteredHandler {
    fun unregister()
}

fun Iterable<RegisteredHandler>.unregisterAll() = forEach { it.unregister() }
