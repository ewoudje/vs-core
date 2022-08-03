package org.valkyrienskies.test_utils.generators

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.doubleArray
import io.kotest.property.arbitrary.numericDouble
import io.kotest.property.exhaustive.exhaustive
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Vector3d

fun Arb.Companion.doubleArray(length: Int, content: Arb<Double> = numericDouble()) =
    doubleArray(listOf(length).exhaustive(), content)

fun Arb.Companion.vector3d(): Arb<Vector3d> = arbitrary {
    Vector3d(numericDouble().bind(), numericDouble().bind(), numericDouble().bind())
}

fun Arb.Companion.matrix4d(): Arb<Matrix4d> = arbitrary {
    Matrix4d().set(doubleArray(16).bind())
}

fun Arb.Companion.quatd(): Arb<Quaterniond> = arbitrary {
    Quaterniond(numericDouble().bind(), numericDouble().bind(), numericDouble().bind(), numericDouble().bind())
}
