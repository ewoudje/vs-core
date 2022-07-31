package org.valkyrienskies.core.pipelines

import org.valkyrienskies.physics_api.PoseVel
import org.valkyrienskies.physics_api.Segment
import org.valkyrienskies.physics_api.SegmentDisplacement
import org.valkyrienskies.physics_api.SegmentTracker
import org.valkyrienskies.physics_api.SingleSegmentTracker

object SegmentUtils {
    fun createSegmentDisplacementFromScaling(dimension: Int, scaling: Double): SegmentDisplacement {
        return SegmentDisplacement(PoseVel.NULL_POSE_VEL, scaling, dimension)
    }

    fun createSegmentTrackerFromScaling(dimension: Int, scaling: Double): SegmentTracker {
        return SingleSegmentTracker(Segment(0, createSegmentDisplacementFromScaling(dimension, scaling)))
    }
}
