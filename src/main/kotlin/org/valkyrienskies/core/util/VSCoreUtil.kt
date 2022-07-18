package org.valkyrienskies.core.util

import com.google.common.collect.ImmutableSet
import io.netty.buffer.ByteBuf
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import java.nio.ByteBuffer
import java.util.function.Consumer

fun Int.squared(): Int = this * this
fun Double.squared(): Double = this * this

/**
 * If this is a `Consumer<Animal>`, for example, then it should be assignable to
 * a `Consumer<Dog>`, but that's not how this works in Java because variance is
 * specified at usage, not at declaration (?!). That means people will naively write
 * `Consumer<Dog>` when they meant to use `Consumer<? super Dog>`. This method
 * allows you to easily circumvent that.
 */
fun <A, B : A> Consumer<A>.variance(): Consumer<B> {
    @Suppress("UNCHECKED_CAST")
    return this as Consumer<B>
}

fun <T> Sequence<T>.toImmutableSet(): ImmutableSet<T> =
    ImmutableSet.builder<T>().also { builder -> this.forEach { builder.add(it) } }.build()

fun <T> Iterable<T>.toImmutableSet(): ImmutableSet<T> = ImmutableSet.copyOf(this)

fun ByteBuf.readVec3fAsDouble(): Vector3d {
    return Vector3d(readFloat().toDouble(), readFloat().toDouble(), readFloat().toDouble())
}

fun ByteBuf.writeVec3AsFloat(v: Vector3dc) {
    writeFloat(v.x().toFloat())
    writeFloat(v.y().toFloat())
    writeFloat(v.z().toFloat())
}

fun ByteBuf.readQuatfAsDouble(): Quaterniond {
    return Quaterniond(readFloat().toDouble(), readFloat().toDouble(), readFloat().toDouble(), readFloat().toDouble())
}

fun ByteBuf.writeQuatfAsFloat(q: Quaterniondc) {
    writeFloat(q.x().toFloat())
    writeFloat(q.y().toFloat())
    writeFloat(q.z().toFloat())
    writeFloat(q.w().toFloat())
}

fun ByteBuf.writeQuatd(q: Quaterniondc) {
    writeDouble(q.x())
    writeDouble(q.y())
    writeDouble(q.z())
    writeDouble(q.w())
}

fun ByteBuf.readQuatd(): Quaterniond {
    return Quaterniond(readDouble(), readDouble(), readDouble(), readDouble())
}

fun ByteBuf.writeVec3d(v: Vector3dc) {
    writeDouble(v.x())
    writeDouble(v.y())
    writeDouble(v.z())
}

fun ByteBuf.readVec3d(): Vector3d {
    return Vector3d(readDouble(), readDouble(), readDouble())
}

/**
 * Returns an array containing the results of applying the
 * given [transform] function to each element in the original array.
 */
inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> =
    Array(size) { index -> transform(this[index]) }

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Array<T?>.filterNotNullToArray(): Array<T> =
    this.filterToArray { it != null } as Array<T>

/**
 * Note: This is NOT threadsafe, do not use this on an array that might be modified from another thread
 */
inline fun <reified T> Array<T>.filterToArray(predicate: (T) -> Boolean): Array<T> {
    val newArray = arrayOfNulls<T>(this.count(predicate))
    var count = 0

    try {
        for (item in this)
            if (predicate(item))
                newArray[count++] = item
    } catch (ex: IndexOutOfBoundsException) {
        throw ConcurrentModificationException("Array was modified while filtering", ex)
    }

    if (count != newArray.size)
        throw ConcurrentModificationException("Array was modified while filtering")

    // Unchecked cast is valid because because if the whole array were not filled we would have thrown already
    @Suppress("UNCHECKED_CAST")
    return newArray as Array<T>
}

fun asLong(x: Int, y: Int): Long = x.toLong() or (y.toLong() shl 32)
fun asInts(x: Long): Pair<Int, Int> = Pair(x.toInt(), (x shr 32).toInt())

inline fun Byte.iterateBits(func: (Boolean, Int) -> Unit) {
    for (i in 8 downTo 0) {
        val masked = (this.toInt() and (1 shl i))
        func(masked != 0, i)
    }
}

inline fun Int.iterateBits(func: (Boolean, Int) -> Unit) {
    for (i in 32 downTo 0) {
        val masked = this and (1 shl i)
        func(masked != 0, i)
    }
}

inline fun Long.iterateBits(func: (Boolean, Int) -> Unit) {
    for (i in 64 downTo 0) {
        val masked = this and (1L shl i)
        func(masked != 0L, i)
    }
}

/**
 * For example ByteBuffer is 01110 then this is called with
 * (false, 0), (true, 1), (true, 2), (true, 3), (false, 4)
 */
inline fun ByteBuffer.iterateBits(func: (Boolean, Int) -> Unit) {
    for (i in 0..this.capacity()) {
        val byte = this.get(i)
        byte.iterateBits { bit, j -> func(bit, i * 8 + j) }
    }
}

/**
 * Take (x, y, z) and produce index (i)
 */
fun unwrapIndex(index: Int, dimensions: Vector3ic, v: Vector3i): Vector3i {
    val z = index / (dimensions.x * dimensions.y)
    val y = (index - (z * dimensions.x * dimensions.y)) / dimensions.x
    val x = (index - (z * dimensions.x * dimensions.y)) % dimensions.x

    return v.set(x, y, z)
}

fun wrapIndex(x: Int, y: Int, z: Int, dimensions: Vector3ic): Int =
    x + (y * dimensions.x) + (z * dimensions.x * dimensions.y)

fun wrapIndex(point: Vector3ic, dimensions: Vector3ic): Int =
    wrapIndex(point.x, point.y, point.z, dimensions)

inline fun timeNanos(func: () -> Unit): Long {
    val start = System.nanoTime()
    func()
    return System.nanoTime() - start
}

inline fun timeMillis(func: () -> Unit): Long {
    val start = System.currentTimeMillis()
    func()
    return System.currentTimeMillis() - start
}

inline fun tryAndPrint(func: () -> Unit) {
    try {
        func()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}
