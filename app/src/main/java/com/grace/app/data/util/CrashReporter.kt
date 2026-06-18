package com.grace.app.data.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.grace.app.BuildConfig

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

    fun log(message: String) {
        if (BuildConfig.DEBUG) Log.d(LOGCAT_TAG, message)
        runCatching { FirebaseCrashlytics.getInstance().log(message) }
    }

    private const val LOGCAT_TAG = "GRACE"

    fun recordNonFatal(t: Throwable?) {
        if (t == null) return
        if (t is kotlinx.coroutines.CancellationException) return
        if (BuildConfig.DEBUG) Log.w(LOGCAT_TAG, "non-fatal", t)
        runCatching { FirebaseCrashlytics.getInstance().recordException(t) }
    }
}
