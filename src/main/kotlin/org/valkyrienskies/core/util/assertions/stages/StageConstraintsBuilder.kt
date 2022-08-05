package org.valkyrienskies.core.util.assertions.stages

import org.valkyrienskies.core.util.assertions.stages.constraints.StageConstraint
import org.valkyrienskies.core.util.assertions.stages.predicates.StagePredicate
import java.util.function.Predicate

class StageConstraintsBuilder<S> {
    private val constraints = ArrayList<StageConstraint<S>>()

    fun constraint(constraint: StageConstraint<S>) {
        constraints.add(constraint)
    }

    fun requireStages(vararg stages: S) {
        constraints.add(StageConstraint.requireStages(*stages))
    }

    fun requireOrder(vararg stages: S) {
        constraints.add(StageConstraint.requireOrder(*stages))
    }

    fun requireOrder(vararg stages: StagePredicate<S>) {
        constraints.add(StageConstraint.requireOrder(*stages))
    }

    fun requireOrder(block: StagePredicatesBuilder<S>.() -> Unit) {
        val predicates = StagePredicatesBuilder<S>().apply(block).build()
        constraints.add(StageConstraint.requireOrder(predicates))
    }

    fun requireExact(vararg stages: S) {
        constraints.add(StageConstraint.requireExact(*stages))
    }

    fun requireNoDuplicates(vararg stages: S) {
        constraints.add(StageConstraint.requireNoDuplicates(*stages))
    }

    fun requireStagesAndOrder(vararg stages: S) {
        constraints.add(StageConstraint.requireStagesAndOrder(*stages))
    }

    fun requireExactOrder(vararg stages: S) {
        constraints.add(StageConstraint.requireExactOrder(*stages))
    }

    fun requireThread(thread: Predicate<Thread>, vararg stages: S) {
        constraints.add(StageConstraint.requireThread(thread, *stages))
    }

    fun build(): List<StageConstraint<S>> {
        return constraints
    }
}
