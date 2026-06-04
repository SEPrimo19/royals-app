package com.grace.app.domain.model

/** A lifeline kind — used by the use_lifeline RPC and the UI buttons. */
enum class LifelineKind {
    /** 🛡️ Joshua Effect — freezes the active Practice timer. */
    JOSHUA,
    /** 🕯️ Daniel Effect — 50/50, eliminates 2 wrong MCQ options. */
    DANIEL;

    val dbValue: String get() = when (this) {
        JOSHUA -> "joshua"
        DANIEL -> "daniel"
    }
}

/** Per-user lifeline balance, refreshes nightly to 3/3. */
data class LifelinesState(
    val joshuaRemaining: Int = 0,
    val danielRemaining: Int = 0
) {
    fun remainingFor(kind: LifelineKind): Int = when (kind) {
        LifelineKind.JOSHUA -> joshuaRemaining
        LifelineKind.DANIEL -> danielRemaining
    }
}
