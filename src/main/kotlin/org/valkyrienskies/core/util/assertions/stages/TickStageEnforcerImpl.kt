package org.valkyrienskies.core.util.assertions.stages

import org.valkyrienskies.core.util.assertions.stages.constraints.ConstraintFailedException
import org.valkyrienskies.core.util.assertions.stages.constraints.StageConstraint

class TickStageEnforcerImpl<S>(
    private val constraints: List<StageConstraint<S>>
) : TickStageEnforcer<S> {

    private val stagesSinceReset = ArrayList<S>()
    private var resetStage: S? = null

    override fun stage(stage: S, reset: Boolean) {
        val isFirstStage = resetStage == null

        if (isFirstStage) {
            resetStage = stage
        }

        if (!reset && isFirstStage) {
            throw IllegalArgumentException("First executed stage must be a reset!")
        }

        if (!reset && stage == resetStage) {
            throw IllegalArgumentException("Stage $stage was previously used as a reset stage but now is not.")
        }

        if (reset && stage != resetStage) {
            throw IllegalArgumentException(
                "Enforcer must be reset on the same stage each time! " +
                    "Was reset on $stage, last reset on $resetStage"
            )
        }

        val errors = mutableListOf<String>()

        if (reset && !isFirstStage) {
            constraints.mapNotNullTo(errors) { it.check(stagesSinceReset, true) }
            stagesSinceReset.clear()
        }

        stagesSinceReset.add(stage)
        constraints.mapNotNullTo(errors) { it.check(stagesSinceReset, false) }


        if (errors.isNotEmpty()) {
            throw ConstraintFailedException(
                "Constraints failed. Stages since last reset: $stagesSinceReset" +
                    "\n${errors.joinToString("\n")}"
            )
        }
    }
}
