package com.grace.app.data.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.grace.app.BuildConfig

/**
 * Thin wrapper over Firebase Crashlytics. Three reasons it exists:
 *
 * 1. **One import surface.** The rest of the codebase calls
 *    `CrashReporter.recordNonFatal(e)` instead of pulling in Firebase
 *    types in domain/data layers, keeping coupling tight.
 * 2. **Safe to call before init.** `FirebaseCrashlytics.getInstance()` is
 *    auto-initialized by the SDK on app start, but calls inside Hilt
 *    constructors run early. We swallow any pre-init NPE so a logging
 *    helper can never itself crash the app.
 * 3. **Coroutine-cancellation aware.** Cancellation is not a crash —
 *    we always skip it. Calling `recordException` on every cancellation
 *    would pollute the dashboard.
 */
object CrashReporter {

    fun setUserId(userId: String?) {
        runCatching {
            FirebaseCrashlytics.getInstance()
                .setUserId(userId.orEmpty())
        }
    }

    fun setKey(key: String, value: String) {
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }
    }

    /**
     * Drop a breadcrumb visible in any future crash report. In debug builds
     * we ALSO emit to android.util.Log so you can `adb logcat | findstr GRACE`
     * and see the trace in real time — Crashlytics has a 5-min pipeline,
     * Logcat is instant.
     */
    fun log(message: String) {
        if (BuildConfig.DEBUG) Log.d(LOGCAT_TAG, message)
        runCatching { FirebaseCrashlytics.getInstance().log(message) }
    }

    private const val LOGCAT_TAG = "GRACE"

    /**
     * Record a non-fatal — the app keeps running but Crashlytics logs the
     * stack so we have a paper trail for "user saw a friendly error
     * message" moments. Coroutine cancellation is filtered out because
     * it's not a bug.
     */
    fun recordNonFatal(t: Throwable?) {
        if (t == null) return
        if (t is kotlinx.coroutines.CancellationException) return
        if (BuildConfig.DEBUG) Log.w(LOGCAT_TAG, "non-fatal", t)
        runCatching { FirebaseCrashlytics.getInstance().recordException(t) }
    }
}
