package org.valkyrienskies.core.pipelines

import org.joml.Vector3dc
import org.valkyrienskies.core.game.ships.ShipId
import org.valkyrienskies.physics_api.RigidBodyInertiaData
import org.valkyrienskies.physics_api.RigidBodyTransform
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate

/**
 * A [VSPhysicsFrame] represents the state of all the bodies in the physics engine. It also has [voxelUpdatesMap] which
 * describes any changes the physics engine made to the voxels.
 */
data class VSPhysicsFrame(
    val shipDataMap: Map<ShipId, ShipInPhysicsFrameData>,
    val voxelUpdatesMap: Map<ShipId, List<IVoxelShapeUpdate>>
)

data class ShipInPhysicsFrameData(
    val uuid: ShipId,
    val dimensionId: Int,
    val inertiaData: RigidBodyInertiaData,
    val shipTransform: RigidBodyTransform,
    val shipVoxelOffset: Vector3dc, // The voxel offset of the ship at this physics frame
    val vel: Vector3dc,
    val omega: Vector3dc
)
