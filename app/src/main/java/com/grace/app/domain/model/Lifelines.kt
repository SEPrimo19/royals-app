package com.grace.app.domain.model

enum class LifelineKind {
    JOSHUA,
    DANIEL;

    val dbValue: String get() = when (this) {
        JOSHUA -> "joshua"
        DANIEL -> "daniel"
    }
}

data class LifelinesState(
    val joshuaRemaining: Int = 0,
    val danielRemaining: Int = 0
) {
    fun remainingFor(kind: LifelineKind): Int = when (kind) {
        LifelineKind.JOSHUA -> joshuaRemaining
        LifelineKind.DANIEL -> danielRemaining
    }
}
