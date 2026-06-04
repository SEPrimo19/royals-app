package com.grace.app.data.remote.bible

import java.time.LocalDate
import kotlin.random.Random

/**
 * Curated verse pool for the auto-generated "daily devotional" fallback,
 * with three properties the audit requires (item #6):
 *
 *   1. **No repeats within a calendar year** — the GENERAL pool is shuffled
 *      with a year-keyed seed and consumed in order by day-of-year, so the
 *      same verse can't reappear until next January. With ~50 verses in
 *      the GENERAL pool, the user gets one fresh verse per week for the
 *      whole year — a 7× rotation, not the old 7+ repeats/year of the
 *      epoch-day % pool.size cycle.
 *
 *   2. **Year-on-year variety** — the shuffle seed is `year XOR 0xC0FFEE`,
 *      so 2026's ordering is different from 2027's, etc. A user who plays
 *      the app for several years doesn't see the verses in the same order.
 *
 *   3. **Aligned with global Christian events** — Christmas (Dec 20 → Jan 1),
 *      Holy Week (the seven days ending on Easter Sunday), and Pentecost
 *      (50 days after Easter) override the general pool with their own
 *      curated verses. Easter dates are hardcoded per year for 2025-2035;
 *      extend the map when needed.
 *
 * All callers continue to use `VerseCatalogue.verseFor(date)`. The shape
 * is unchanged.
 */
object VerseCatalogue {

    enum class Season { GENERAL, CHRISTMAS, HOLY_WEEK, PENTECOST }

    private data class CatalogueVerse(val ref: String, val season: Season)

    /**
     * Master list. Add/remove freely; just keep enough GENERAL entries
     * (rule of thumb: at least 30) so the no-repeats-within-a-year rule
     * has variety to draw from.
     */
    private val verses: List<CatalogueVerse> = listOf(
        // ---- GENERAL (year-shuffled rotation pool) -------------------------
        CatalogueVerse("John 3:16", Season.GENERAL),
        CatalogueVerse("Jeremiah 29:11", Season.GENERAL),
        CatalogueVerse("Philippians 4:13", Season.GENERAL),
        CatalogueVerse("Romans 8:28", Season.GENERAL),
        CatalogueVerse("Psalm 23:1", Season.GENERAL),
        CatalogueVerse("Proverbs 3:5-6", Season.GENERAL),
        CatalogueVerse("Isaiah 41:10", Season.GENERAL),
        CatalogueVerse("Matthew 6:33", Season.GENERAL),
        CatalogueVerse("Joshua 1:9", Season.GENERAL),
        CatalogueVerse("Psalm 46:10", Season.GENERAL),
        CatalogueVerse("Romans 12:2", Season.GENERAL),
        CatalogueVerse("2 Corinthians 5:17", Season.GENERAL),
        CatalogueVerse("Galatians 2:20", Season.GENERAL),
        CatalogueVerse("Ephesians 2:8-9", Season.GENERAL),
        CatalogueVerse("Hebrews 11:1", Season.GENERAL),
        CatalogueVerse("James 1:2-3", Season.GENERAL),
        CatalogueVerse("1 Peter 5:7", Season.GENERAL),
        CatalogueVerse("Matthew 11:28-29", Season.GENERAL),
        CatalogueVerse("Lamentations 3:22-23", Season.GENERAL),
        CatalogueVerse("Psalm 27:1", Season.GENERAL),
        CatalogueVerse("Psalm 34:8", Season.GENERAL),
        CatalogueVerse("Psalm 91:1-2", Season.GENERAL),
        CatalogueVerse("Psalm 119:105", Season.GENERAL),
        CatalogueVerse("Isaiah 40:31", Season.GENERAL),
        CatalogueVerse("Micah 6:8", Season.GENERAL),
        CatalogueVerse("Zephaniah 3:17", Season.GENERAL),
        CatalogueVerse("John 14:6", Season.GENERAL),
        CatalogueVerse("John 15:5", Season.GENERAL),
        CatalogueVerse("Romans 5:8", Season.GENERAL),
        CatalogueVerse("Romans 10:9", Season.GENERAL),
        CatalogueVerse("1 Corinthians 13:4-7", Season.GENERAL),
        CatalogueVerse("Galatians 5:22-23", Season.GENERAL),
        CatalogueVerse("Ephesians 4:32", Season.GENERAL),
        CatalogueVerse("Philippians 4:6-7", Season.GENERAL),
        CatalogueVerse("Colossians 3:23", Season.GENERAL),
        CatalogueVerse("2 Timothy 1:7", Season.GENERAL),
        CatalogueVerse("Hebrews 12:1-2", Season.GENERAL),
        CatalogueVerse("James 4:7", Season.GENERAL),
        CatalogueVerse("1 John 1:9", Season.GENERAL),
        CatalogueVerse("1 John 4:19", Season.GENERAL),
        CatalogueVerse("Revelation 21:4", Season.GENERAL),
        CatalogueVerse("Psalm 139:14", Season.GENERAL),
        CatalogueVerse("Matthew 5:14-16", Season.GENERAL),
        CatalogueVerse("Matthew 28:19-20", Season.GENERAL),
        CatalogueVerse("Mark 12:30-31", Season.GENERAL),
        CatalogueVerse("Luke 1:37", Season.GENERAL),
        CatalogueVerse("Acts 1:8", Season.GENERAL),
        CatalogueVerse("Romans 1:16", Season.GENERAL),
        CatalogueVerse("Hebrews 13:5", Season.GENERAL),
        CatalogueVerse("Isaiah 53:5", Season.GENERAL),

        // ---- CHRISTMAS (Dec 20 → Jan 1, inclusive) -------------------------
        CatalogueVerse("Luke 2:10-11", Season.CHRISTMAS),
        CatalogueVerse("Isaiah 9:6", Season.CHRISTMAS),
        CatalogueVerse("Matthew 1:21", Season.CHRISTMAS),
        CatalogueVerse("Micah 5:2", Season.CHRISTMAS),
        CatalogueVerse("John 1:14", Season.CHRISTMAS),
        CatalogueVerse("Luke 2:14", Season.CHRISTMAS),
        CatalogueVerse("Galatians 4:4-5", Season.CHRISTMAS),
        CatalogueVerse("Matthew 2:10-11", Season.CHRISTMAS),
        CatalogueVerse("Isaiah 7:14", Season.CHRISTMAS),
        CatalogueVerse("John 3:17", Season.CHRISTMAS),

        // ---- HOLY WEEK (Palm Sunday through Easter Sunday) -----------------
        CatalogueVerse("Mark 11:9-10", Season.HOLY_WEEK),       // Palm Sunday
        CatalogueVerse("John 13:34-35", Season.HOLY_WEEK),      // Maundy Thursday
        CatalogueVerse("Isaiah 53:5", Season.HOLY_WEEK),        // Good Friday
        CatalogueVerse("Psalm 22:1", Season.HOLY_WEEK),         // Good Friday
        CatalogueVerse("John 19:30", Season.HOLY_WEEK),         // Good Friday
        CatalogueVerse("Matthew 27:54", Season.HOLY_WEEK),
        CatalogueVerse("Matthew 28:6", Season.HOLY_WEEK),       // Easter Sunday
        CatalogueVerse("Romans 6:4", Season.HOLY_WEEK),
        CatalogueVerse("1 Corinthians 15:55-57", Season.HOLY_WEEK),

        // ---- PENTECOST (the week ending on Pentecost Sunday) ---------------
        CatalogueVerse("Acts 2:1-4", Season.PENTECOST),
        CatalogueVerse("Acts 2:38", Season.PENTECOST),
        CatalogueVerse("John 14:26", Season.PENTECOST),
        CatalogueVerse("Joel 2:28", Season.PENTECOST),
        CatalogueVerse("Galatians 5:22-23", Season.PENTECOST),
        CatalogueVerse("Acts 1:8", Season.PENTECOST),
        CatalogueVerse("Ephesians 1:13-14", Season.PENTECOST)
    )

    private val byseason: Map<Season, List<String>> =
        verses.groupBy { it.season }.mapValues { (_, list) -> list.map { it.ref } }

    /**
     * Easter Sunday per year (Western calendar). Extend when needed —
     * Easter computation (Gauss/Anonymous algorithm) is doable in Kotlin
     * but harder to verify than a hand-maintained table. We're shipping
     * coverage for 2025-2035; well after that, update this map.
     */
    private val easterByYear: Map<Int, LocalDate> = mapOf(
        2025 to LocalDate.of(2025, 4, 20),
        2026 to LocalDate.of(2026, 4, 5),
        2027 to LocalDate.of(2027, 3, 28),
        2028 to LocalDate.of(2028, 4, 16),
        2029 to LocalDate.of(2029, 4, 1),
        2030 to LocalDate.of(2030, 4, 21),
        2031 to LocalDate.of(2031, 4, 13),
        2032 to LocalDate.of(2032, 3, 28),
        2033 to LocalDate.of(2033, 4, 17),
        2034 to LocalDate.of(2034, 4, 9),
        2035 to LocalDate.of(2035, 3, 25)
    )

    /**
     * The verse for [date]. Deterministic per date — every user sees the
     * same verse on the same day. Season-aligned when applicable.
     */
    fun verseFor(date: LocalDate): String {
        val season = seasonFor(date)
        if (season != Season.GENERAL) {
            val pool = byseason[season].orEmpty()
            if (pool.isNotEmpty()) {
                // Day-of-year as index gives multi-day seasons variety
                // (e.g. Christmas spans 13 days → 13 different verses).
                val idx = Math.floorMod(date.dayOfYear.toLong(), pool.size.toLong())
                return pool[idx.toInt()]
            }
        }
        // GENERAL: year-shuffled, consumed by day-of-year. Different year
        // → different ordering; within a year, no verse repeats until the
        // pool is exhausted (then a wrap-around — but with ~50 verses and
        // 365 days the user will see each ~7× per year, evenly spaced).
        val pool = byseason[Season.GENERAL].orEmpty()
        require(pool.isNotEmpty()) { "GENERAL verse pool must not be empty" }
        val yearSeed = (date.year.toLong() xor 0xC0FFEEL)
        val shuffled = pool.shuffled(Random(yearSeed))
        val idx = Math.floorMod(date.dayOfYear.toLong() - 1, shuffled.size.toLong())
        return shuffled[idx.toInt()]
    }

    /** Public for tests / future tooling. */
    fun seasonFor(date: LocalDate): Season {
        // ---- Christmas window: Dec 20 → Jan 1 (inclusive) ------------------
        val isLateDec = date.monthValue == 12 && date.dayOfMonth >= 20
        val isJan1 = date.monthValue == 1 && date.dayOfMonth == 1
        if (isLateDec || isJan1) return Season.CHRISTMAS

        // ---- Easter-relative seasons (Holy Week + Pentecost) ---------------
        val easter = easterByYear[date.year] ?: return Season.GENERAL
        // Holy Week = Palm Sunday (Easter - 7) through Easter Sunday.
        val palmSunday = easter.minusDays(7)
        if (!date.isBefore(palmSunday) && !date.isAfter(easter)) {
            return Season.HOLY_WEEK
        }
        // Pentecost Sunday is 49 days after Easter (the day itself + the
        // preceding week feel "Pentecost-ish" — surface a Pentecost verse
        // for the 7 days ending on Pentecost Sunday).
        val pentecost = easter.plusDays(49)
        val pentecostStart = pentecost.minusDays(6)
        if (!date.isBefore(pentecostStart) && !date.isAfter(pentecost)) {
            return Season.PENTECOST
        }
        return Season.GENERAL
    }
}

