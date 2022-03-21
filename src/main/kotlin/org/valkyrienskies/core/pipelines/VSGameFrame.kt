package org.valkyrienskies.core.pipelines

import org.joml.Vector3ic
import org.valkyrienskies.physics_api.RigidBodyInertiaData
import org.valkyrienskies.physics_api.RigidBodyTransform
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate
import java.util.*

/**
 * A [VSGameFrame] represents the change of state of the game that occurred over 1 tick
 */
data class VSGameFrame(
    val newShips: List<NewShipInGameFrameData>, // Ships to be added to the Physics simulation
    val deletedShips: List<UUID>, // Ships to be deleted from the Physics simulation
    val voxelUpdatesMap: Map<UUID, List<IVoxelShapeUpdate>> // Voxel updates applied by this frame
)

/**
 * The data used to add a new ship to the physics engine
 */
data class NewShipInGameFrameData(
    val uuid: UUID,
    val dimensionId: Int,
    val minDefined: Vector3ic,
    val maxDefined: Vector3ic,
    val inertiaData: RigidBodyInertiaData,
    val shipTransform: RigidBodyTransform
)
