package org.valkyrienskies.core.util.assertions.stages

import org.valkyrienskies.core.util.assertions.stages.predicates.StagePredicate

class StagePredicatesBuilder<S> {
    private val predicates = ArrayList<StagePredicate<S>>()

    fun matches(predicate: StagePredicate<S>) {
        predicates.add(predicate)
    }

    fun single(stage: S) {
        predicates.add(StagePredicate.single(stage))
    }

    fun oneOf(vararg stages: S) {
        predicates.add(StagePredicate.oneOf(*stages))
    }

    fun build(): List<StagePredicate<S>> {
        return predicates.toList()
    }
}
