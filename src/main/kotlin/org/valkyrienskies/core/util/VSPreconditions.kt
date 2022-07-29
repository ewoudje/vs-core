package org.valkyrienskies.core.util

fun assertIsPhysicsThread() {
    assert(Thread.currentThread().name.startsWith("Physics thread")) { "Not called from physics thread" }
}
