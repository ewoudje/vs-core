package org.valkyrienskies.core.physics.nodes.control

import org.valkyrienskies.core.physics.nodes.EngineNode

abstract class ControlLoop(engineNodes: Collection<EngineNode> = emptyList()) {
    abstract fun tick(deltaNs: Long)

    private val _engineNodes = ArrayList<EngineNode>(engineNodes)
    val engineNodes: List<EngineNode> get() = _engineNodes

    fun addEngineNode(node: EngineNode) {
        onAddEngineNode(node)
        _engineNodes += node
    }

    fun removeEngineNode(node: EngineNode) {
        onRemoveEngineNode(node)
        _engineNodes -= node
    }

    protected open fun onAddEngineNode(node: EngineNode) {}
    protected open fun onRemoveEngineNode(node: EngineNode) {}
}