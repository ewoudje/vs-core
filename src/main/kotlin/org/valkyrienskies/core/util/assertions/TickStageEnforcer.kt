package org.valkyrienskies.core.util.assertions

import java.util.function.Predicate

class TickStageEnforcer<S>(
    private vararg val constraints: Constraint<S>
) {
    private val stagesSinceReset = ArrayList<S>()
    private var lastResetStage: S? = null
    private var isFirstStage = true

    fun stage(stage: S, reset: Boolean = false) {
        if (!reset && isFirstStage) {
            throw ConstraintFailedException("First executed stage must be a reset!")
        }

        if (reset && lastResetStage != null && stage != lastResetStage) {
            throw ConstraintFailedException(
                "Enforcer must be reset on the same stage each time! " +
                    "Was reset on $stage, last reset on $lastResetStage"
            )
        }

        stagesSinceReset.add(stage)
        val errors = constraints.mapNotNull { it.check(stagesSinceReset, reset && !isFirstStage) }

        if (reset) {
            stagesSinceReset.clear()
            lastResetStage = stage
        }

        if (isFirstStage) {
            isFirstStage = false
        }

        if (errors.isNotEmpty()) {
            throw ConstraintFailedException(
                "Constraints failed for stages: $stagesSinceReset" +
                    "\n${errors.joinToString("\n")}"
            )
        }
    }

    class ConstraintFailedException(message: String) : Exception(message)

    interface Constraint<S> {
        fun check(stagesSinceReset: List<S>, isReset: Boolean): String?
    }

    object Constraints {
        fun <S> requireStages(vararg stages: S): Constraint<S> = RequireStages(*stages)
        fun <S> requireOrder(vararg stages: S): Constraint<S> = RequireOrder(*stages)

        fun <S> requireStagesAndOrder(vararg stages: S): Constraint<S> =
            Compose(requireOrder(*stages), requireStages(*stages))
        
        fun <S> requireThread(thread: Predicate<Thread>, vararg stages: S): Constraint<S> =
            RequireThread(thread, *stages)
    }

    class Compose<S>(private vararg val constraints: Constraint<S>) : Constraint<S> {
        override fun check(stagesSinceReset: List<S>, isReset: Boolean): String? {
            val checks = constraints.mapNotNull { it.check(stagesSinceReset, isReset) }

            return when {
                checks.size > 1 -> "Composite failure: " + checks.joinToString()
                checks.size == 1 -> checks.first()
                else -> null
            }
        }
    }

    class RequireStages<S>(vararg stages: S) : Constraint<S> {
        private val stages: List<S> = stages.asList()

        override fun check(stagesSinceReset: List<S>, isReset: Boolean): String? {
            if (isReset && !stagesSinceReset.containsAll(stages)) {
                return "Required all stages $stages"
            }
            return null
        }
    }

    class RequireOrder<S>(vararg stages: S) : Constraint<S> {
        private val stages: List<S> = stages.asList()

        override fun check(stagesSinceReset: List<S>, isReset: Boolean): String? {
            var requiredIndex = 0
            for (stage in stagesSinceReset) {
                if (stages.contains(stage) && stage != stages[requiredIndex++]) {
                    return "Required stages in the following order: $stages"
                }
            }
            return null
        }
    }

    class RequireThread<S>(
        private val checkThread: Predicate<Thread>,
        private vararg val stages: S
    ) : Constraint<S> {
        override fun check(stagesSinceReset: List<S>, isReset: Boolean): String? {
            if (stages.contains(stagesSinceReset.last()) && !checkThread.test(Thread.currentThread())) {
                return "Stages $stages require a different thread. Current thread: ${Thread.currentThread()}"
            }

            return null
        }
    }
}
