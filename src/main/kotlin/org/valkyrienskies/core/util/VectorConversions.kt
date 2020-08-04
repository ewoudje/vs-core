package org.valkyrienskies.core.util

import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import org.joml.*
import java.lang.Math.abs

// region JOML

// Vector3ic
operator fun Vector3ic.component1() = x
operator fun Vector3ic.component2() = y
operator fun Vector3ic.component3() = z

val Vector3ic.x get() = x()
val Vector3ic.y get() = y()
val Vector3ic.z get() = z()

fun Vector3ic.multiplyTerms() = x * y * z

// Vector3dc
fun Vector3dc.toGDX() = assignTo(Vector3())
infix fun Vector3dc.assignTo(v: Vector3): Vector3 {
    v.x = x.toFloat()
    v.y = y.toFloat()
    v.z = z.toFloat()
    return v
}
infix fun Vector3dc.assignTo(v: Vector3d) = v.set(this)

operator fun Vector3dc.component1() = x
operator fun Vector3dc.component2() = y
operator fun Vector3dc.component3() = z

val Vector3dc.x get() = x()
val Vector3dc.y get() = y()
val Vector3dc.z get() = z()

fun Vector3dc.multiplyTerms() = x * y * z
fun Vector3dc.addTerms() = x + y + z

// Matrix4dc
fun Matrix4dc.toGDX() = assignTo(Matrix4())
infix fun Matrix4dc.assignTo(m: Matrix4): Matrix4 {
    this.get(m.values)
    return m
}

// Matrix3dc

fun Matrix3dc.toGDX() = assignTo(Matrix3())
infix fun Matrix3dc.assignTo(m: Matrix3): Matrix3 {
    this.get(m.values)
    return m
}
// endregion

// region GDX

// Vector3
infix fun Vector3.assignTo(v: Vector3d): Vector3d {
    v.x = x.toDouble()
    v.y = y.toDouble()
    v.z = z.toDouble()
    return v
}
infix fun Vector3.assignTo(v: Vector3i): Vector3i {
    v.x = x.toInt()
    v.y = y.toInt()
    v.z = z.toInt()
    return v
}

// Matrix4
infix fun Matrix4.assignTo(m: Matrix4d): Matrix4d {
    return m.set(this.values)
}

