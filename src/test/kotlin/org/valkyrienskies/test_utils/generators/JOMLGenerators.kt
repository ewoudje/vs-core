package org.valkyrienskies.test_utils.generators

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.numericDouble
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.core.VSRandomUtils

fun Arb.Companion.vector3d(): Arb<Vector3d> = arbitrary {
    Vector3d(numericDouble().bind(), numericDouble().bind(), numericDouble().bind())
}

fun Arb.Companion.matrix4d(): Arb<Matrix4d> = arbitrary {
    Matrix4d().set(doubleArray(16).bind())
}

fun Arb.Companion.quatd(): Arb<Quaterniond> = arbitrary { rs ->
    VSRandomUtils.randomQuaterniond(rs.random)
}
