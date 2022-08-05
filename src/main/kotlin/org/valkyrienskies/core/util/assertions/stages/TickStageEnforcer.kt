package org.valkyrienskies.core.util.assertions.stages

import org.valkyrienskies.core.util.assertions.stages.constraints.StageConstraint

interface TickStageEnforcer<S> {
    fun stage(stage: S, reset: Boolean = false)
}

fun <S> TickStageEnforcer(vararg constraints: StageConstraint<S>): TickStageEnforcer<S> =
    TickStageEnforcerImpl(constraints.asList())

fun <S> TickStageEnforcer(block: StageConstraintsBuilder<S>.() -> Unit): TickStageEnforcer<S> =
    TickStageEnforcerImpl(StageConstraintsBuilder<S>().apply(block).build())

