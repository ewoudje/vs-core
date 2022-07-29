package org.valkyrienskies.core.util

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.reflect.KProperty

fun logger(): DelegateLogger = DelegateLogger

@JvmInline
value class ClassLogger(val logger: Logger) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Logger = logger
}

object DelegateLogger {
    operator fun provideDelegate(thisRef: Any, property: KProperty<*>) =
        ClassLogger(
            LogManager.getLogger(
                if (thisRef::class.isCompanion)
                    thisRef::class.java.declaringClass
                else
                    thisRef::class.java
            )
        )
}
