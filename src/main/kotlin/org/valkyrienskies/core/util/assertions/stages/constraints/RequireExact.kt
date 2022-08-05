package org.valkyrienskies.core.util.assertions.stages.constraints

internal class RequireExact<S> constructor(vararg stages: S) : StageConstraint<S> {
    init {
        require(stages.isNotEmpty())
    }

    private val stages: List<S> = stages.asList()

    override fun check(stagesSinceReset: List<S>, isReset: Boolean): String? {
        if (isReset && (stagesSinceReset.size != stages.size || !stagesSinceReset.containsAll(stages))) {
            return "Required exact stages $stages"
        }
        return null
    }
}
