package org.valkyrienskies.core.util.assertions.stages.constraints

import org.valkyrienskies.core.util.assertions.stages.predicates.StagePredicate
import java.util.function.Predicate

interface StageConstraint<S> {
    fun check(stagesSinceReset: List<S>, isReset: Boolean): String?

    companion object {
        fun <S> requireStages(vararg stages: S): StageConstraint<S> = RequireStages(*stages)
        fun <S> requireOrder(vararg stages: S): StageConstraint<S> = RequireOrder(stages.asList())
        fun <S> requireOrder(stages: List<S>): StageConstraint<S> = RequireOrder(stages.toList())

        fun <S> requireOrder(vararg stages: StagePredicate<S>): StageConstraint<S> =
            RequireOrderPredicate(stages.asList())

        @JvmName("requireOrderList")
        fun <S> requireOrder(stages: List<StagePredicate<S>>): StageConstraint<S> =
            RequireOrderPredicate(stages.toList())

        fun <S> requireExact(vararg stages: S): StageConstraint<S> = RequireExact(*stages)

        fun <S> requireNoDuplicates(vararg stages: S): StageConstraint<S> = RequireNoDuplicates(*stages)

        fun <S> compose(vararg constraints: StageConstraint<S>): StageConstraint<S> = Compose(*constraints)

        fun <S> requireStagesAndOrder(vararg stages: S): StageConstraint<S> =
            compose(requireOrder(*stages), requireStages(*stages))

        fun <S> requireExactOrder(vararg stages: S): StageConstraint<S> =
            compose(requireOrder(*stages), requireExact(*stages))

        fun <S> requireThread(thread: Predicate<Thread>, vararg stages: S): StageConstraint<S> =
            RequireThread(thread, *stages)
    }
}
