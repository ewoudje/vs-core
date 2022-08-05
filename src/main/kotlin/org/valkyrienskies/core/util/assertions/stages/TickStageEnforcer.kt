package org.valkyrienskies.core.util.assertions.stages

import org.valkyrienskies.core.util.assertions.stages.constraints.StageConstraint

interface TickStageEnforcer<S> {
    fun stage(stage: S)
}

fun <S> TickStageEnforcer(resetStage: S, vararg constraints: StageConstraint<S>): TickStageEnforcer<S> =
    TickStageEnforcerImpl(resetStage, constraints.asList())

fun <S> TickStageEnforcer(resetStage: S, block: StageConstraintsBuilder<S>.() -> Unit): TickStageEnforcer<S> =
    TickStageEnforcerImpl(resetStage, StageConstraintsBuilder<S>().apply(block).build())

