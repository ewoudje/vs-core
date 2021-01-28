package org.valkyrienskies.core.util.serialization

import com.fasterxml.jackson.databind.module.SimpleModule
import org.joml.Matrix3d
import org.joml.Matrix3dc
import org.joml.Matrix4d
import org.joml.Matrix4dc
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc

class JOMLSerializationModule: SimpleModule() {
    init {
        super.addAbstractTypeMapping(Vector3dc::class.java, Vector3d::class.java)
        super.addAbstractTypeMapping(Quaterniondc::class.java, Quaterniond::class.java)
        super.addAbstractTypeMapping(Matrix4dc::class.java, Matrix4d::class.java)
        super.addAbstractTypeMapping(Matrix3dc::class.java, Matrix3d::class.java)
        super.addAbstractTypeMapping(Vector3ic::class.java, Vector3i::class.java)
        super.addAbstractTypeMapping(AABBdc::class.java, AABBd::class.java)
    }
}