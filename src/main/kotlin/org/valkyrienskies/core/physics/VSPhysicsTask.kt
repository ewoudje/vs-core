package org.valkyrienskies.core.physics

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

class VSPhysicsTask(val physicsWorld: VSPhysicsWorld) : Runnable {
    // When this is set to true, this task will kill itself at the next opportunity
    private var killTask = false

    // A non-blocking thread-safe queue
    private val queuedTasksQueue = ConcurrentLinkedQueue<() -> Unit>()

    private var lostTime: Long = 0

    private var idealPhysicsTps = 100

    private var runPhysics = true

    override fun run() {
        while (true) {
            if (killTask) break // Stop looping

            val timeToSimulateNs = 1e9 / idealPhysicsTps.toDouble()

            val timeBeforePhysicsTick = System.nanoTime()

            // Execute queued tasks
            while (!queuedTasksQueue.isEmpty()) queuedTasksQueue.remove()()
            // Run the physics tick
            physicsWorld.tick(timeToSimulateNs / 1e9)

            val timeToRunPhysTick = System.nanoTime() - timeBeforePhysicsTick

            // Ideal time minus actual time to run physics tick
            val timeDif = timeToSimulateNs - timeToRunPhysTick

            if (timeDif < 0) {
                // Physics tick took too long, store some lost time to catch up
                lostTime = min(lostTime - timeDif.toLong(), MAX_LOST_TIME)
            } else {
                if (lostTime > timeDif) {
                    // Catch up
                    lostTime -= timeDif.toLong()
                } else {
                    val timeToWait = timeDif - lostTime
                    lostTime = 0
                    Thread.sleep((timeToWait / 1e6).toLong())
                }
            }
        }
        print("Task ending")
    }

    fun tellTaskToKillItself() {
        killTask = true
    }

    fun queueTask(task: () -> Unit) {
        queuedTasksQueue.add(task)
    }

    companion object {
        private const val MAX_LOST_TIME: Long = 1e9.toLong()
    }
}
