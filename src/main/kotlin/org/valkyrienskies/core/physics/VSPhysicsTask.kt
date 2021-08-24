package org.valkyrienskies.core.physics

import java.util.concurrent.ConcurrentLinkedQueue

class VSPhysicsTask(val physicsWorld: VSPhysicsWorld) : Runnable {
    // When this is set to true, this task will kill itself at the next opportunity
    private var killTask = false

    // A non-blocking thread-safe queue
    private val queuedTasksQueue = ConcurrentLinkedQueue<() -> Unit>()

    override fun run() {
        while (true) {
            if (killTask) break // Stop looping

            // Execute queued tasks
            while (!queuedTasksQueue.isEmpty()) queuedTasksQueue.remove()()

            // Simulate 1/100th of a second
            physicsWorld.tick(.01)
            // print("hi!\n")
            Thread.sleep(10)
        }
        print("Task ending")
    }

    fun tellTaskToKillItself() {
        killTask = true
    }

    fun queueTask(task: () -> Unit) {
        queuedTasksQueue.add(task)
    }
}
