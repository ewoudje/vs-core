package org.valkyrienskies.core.api

interface ServerShip : Ship {
    /**
     * Sets data in the persistent storage
     *
     * @param T
     * @param clazz of T
     * @param value the data that will be stored, if null will be removed
     */
    fun <T> saveAttachment(clazz: Class<T>, value: T?)
}

inline fun <reified T> ServerShip.saveAttachment(value: T?) = saveAttachment(T::class.java, value)
