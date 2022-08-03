package org.valkyrienskies.core.util.events

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventEmitterImplTest {

    @Test
    fun testEventListen() {
        val emitter = EventEmitterImpl<String>()
        var called = 0
        emitter.on { value, _ ->
            called++
            assertEquals("hi", value)
        }

        emitter.emit("hi")

        assertEquals(1, called)
    }

    @Test
    fun testEventListenOnce() {
        val emitter = EventEmitterImpl<String>()

        var called = 0

        emitter.once({ it == "hi" }) { value ->
            called++
            assertEquals("hi", value)
        }

        emitter.emit("bye")
        emitter.emit("hi")
        emitter.emit("bye")
        emitter.emit("hi")

        assertEquals(1, called)
    }

    @Test
    fun testEventListenUnregister() {
        val emitter = EventEmitterImpl<String>()

        var called = 0

        emitter.on { value, handler ->
            called++
            if (value == "unregister") {
                handler.unregister()
            }
        }

        emitter.emit("1")
        emitter.emit("2")
        emitter.emit("3")
        emitter.emit("unregister")
        emitter.emit("4")
        emitter.emit("5")

        assertEquals(4, called)
    }
}
