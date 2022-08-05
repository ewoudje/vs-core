package org.valkyrienskies.core.util.assertions

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import org.valkyrienskies.core.util.assertions.TickStageEnforcer.ConstraintFailedException
import org.valkyrienskies.core.util.assertions.TickStageEnforcer.Constraints
import java.util.concurrent.Executors

class TickStageEnforcerTest : StringSpec({

    "requires first stage is a reset" {
        val enforcer = TickStageEnforcer<String>()

        shouldThrow<ConstraintFailedException> {
            enforcer.stage("a")
        }
    }

    "requires reset stage is the same each time" {
        val enforcer = TickStageEnforcer<String>()

        enforcer.stage("a", true)
        shouldThrow<ConstraintFailedException> {
            enforcer.stage("b", true)
        }

        enforcer.stage("a", true)
        enforcer.stage("a", true)
        shouldThrow<ConstraintFailedException> {
            enforcer.stage("b", true)
        }
    }

    "enforces order" {
        val enforcer = TickStageEnforcer(Constraints.requireOrder("a", "b", "c"))

        enforcer.stage("a", true)
        enforcer.stage("d")

        shouldThrow<ConstraintFailedException> {
            enforcer.stage("c")
        }
    }

    "enforces required stages" {
        val enforcer = TickStageEnforcer(Constraints.requireStages("a", "b", "c"))

        enforcer.stage("a", true)
        enforcer.stage("b")
        enforcer.stage("b")

        shouldThrow<ConstraintFailedException> {
            enforcer.stage("a", true)
        }

        enforcer.stage("b")
        enforcer.stage("c")
        enforcer.stage("a", true)
    }

    "enforces required stages and order" {
        val enforcer = TickStageEnforcer(Constraints.requireStagesAndOrder("a", "b", "c"))

        enforcer.stage("0", true)
        enforcer.stage("a")
        enforcer.stage("1")
        enforcer.stage("b")

        shouldThrow<ConstraintFailedException> {
            enforcer.stage("0", true)
        }

        enforcer.stage("a")
        enforcer.stage("1")
        enforcer.stage("b")
        enforcer.stage("c")
        enforcer.stage("0", true)
    }

    "enforces correct thread" {
        val thread = Thread()
        val executor = Executors.newSingleThreadExecutor { thread }

        val enforcer = TickStageEnforcer(Constraints.requireThread({ it == thread }, "b"))

        enforcer.stage("a", true)

        shouldThrow<ConstraintFailedException> {
            enforcer.stage("b")
        }

        executor.submit {
            shouldNotThrow<ConstraintFailedException> {
                enforcer.stage("b")
            }
        }
    }

})
