package org.valkyrienskies.core.pipelines

import org.joml.Vector3dc
import org.valkyrienskies.core.game.ships.ShipInertiaData
import org.valkyrienskies.core.game.ships.ShipTransform
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate
import java.util.*

/**
 * A [VSPhysicsFrame] represents the state of all the bodies in the physics engine. It also has [voxelUpdatesMap] which
 * describes any changes the physics engine made to the voxels.
 */
data class VSPhysicsFrame(
    val shipDataMap: Map<UUID, ShipInPhysicsFrameData>,
    val voxelUpdatesMap: Map<UUID, List<IVoxelShapeUpdate>>
)

data class ShipInPhysicsFrameData(
    val uuid: UUID,
    val dimensionId: Int,
    val inertiaData: ShipInertiaData,
    val shipTransform: ShipTransform,
    val vel: Vector3dc,
    val omega: Vector3dc
)
