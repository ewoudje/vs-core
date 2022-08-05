package org.valkyrienskies.core.util

import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel.DEBUG
import io.sentry.SentryLevel.ERROR
import io.sentry.SentryLevel.FATAL
import io.sentry.SentryLevel.INFO
import io.sentry.SentryLevel.WARNING
import io.sentry.protocol.App
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.message.Message
import org.valkyrienskies.core.config.VSCoreConfig

object DataCollection {

    val isCollecting = true

    init {
        Sentry.init {
            var dsn = ""

            if (dsn == "")
                dsn = VSCoreConfig.SERVER.sentryUrl

            if (dsn == "")
                dsn = VSCoreConfig.CLIENT.sentryUrl


            it.dsn = dsn
            it.tracesSampleRate = 1.0
        }

        Sentry.configureScope {
            it.level = WARNING
            it.contexts.setApp(App().apply {
                appName = "Valkyrien Skies"
                appVersion = "2.0" // TODO get actual number
            })
            it.setContexts("username", "TODO") // TODO get actual username (or shouldn't we?)
        }
    }

    fun log(level: Level, marker: Marker?, message: Message, t: Throwable?) {

        if (Level.ERROR.intLevel() >= level.intLevel()) {
            if (Level.ERROR.intLevel() == level.intLevel())
                Sentry.captureMessage(message.formattedMessage, ERROR)
            else
                Sentry.captureMessage(message.formattedMessage, FATAL)

            if (t != null) {
                Sentry.captureException(t)
            }
        } else {

            if (Level.WARN.intLevel() >= level.intLevel())
                Sentry.captureMessage(message.formattedMessage, WARNING)
            else if (Level.INFO.intLevel() >= level.intLevel())
                Sentry.captureMessage(message.formattedMessage, INFO)
            else if (Level.DEBUG.intLevel() >= level.intLevel())
                Sentry.captureMessage(message.formattedMessage, DEBUG)
            else
                Sentry.addBreadcrumb(Breadcrumb(message.formattedMessage).apply {
                    setData("thread", Thread.currentThread().name)
                    if (marker != null) setData("marker", marker)
                })
        }
    }
}
