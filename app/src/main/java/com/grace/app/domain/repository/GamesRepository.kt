package com.grace.app.domain.repository

import com.grace.app.domain.model.BibleCharacter
import com.grace.app.domain.model.BibleEvent
import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.model.BibleVerseScramble
import com.grace.app.domain.model.GameDifficulty
import com.grace.app.domain.model.LifelineKind
import com.grace.app.domain.model.LifelinesState
import com.grace.app.domain.model.MemoryCardPair
import com.grace.app.domain.model.GameMode
import com.grace.app.domain.model.GameStats
import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.domain.model.QuestionCategory
import com.grace.app.domain.util.Result

/**
 * Bible Games data access. See [[bible-games-v1-design]] for the locked
 * scope. All methods are best-effort against Supabase — there's no Room
 * caching in v1 to keep the surface area small.
 */
interface GamesRepository {

    /**
     * Today's 10-question Daily Challenge set for the given difficulty.
     * Deterministic per (date, difficulty) so every player in the church
     * sees the same set today, keeping the leaderboard apples-to-apples.
     * Each difficulty is an independent round — completing Easy doesn't
     * affect Medium or Hard for the day.
     */
    suspend fun getDailyChallengeQuestions(
        difficulty: GameDifficulty
    ): Result<List<BibleQuestion>>

    /**
     * Big random pool for Practice mode — Practice is "unlimited" so the
     * VM streams from this list, reshuffling when exhausted. Mixed
     * difficulty by default gives the lives/timer mechanic real variety.
     */
    suspend fun getPracticeQuestions(
        category: QuestionCategory? = null,
        count: Int = 40
    ): Result<List<BibleQuestion>>

    /** A single Fill-in-the-Blank passage for the next round. */
    suspend fun getRandomFillInBlank(): Result<BiblePassage?>

    /**
     * Random "Who Am I?" characters for a Practice session. Fetches all
     * active characters then takes a shuffled slice — the ViewModel walks
     * through them one at a time and reshuffles on exhaustion so a session
     * never repeats a character until the entire pool has been seen.
     */
    suspend fun getWhoAmICharacters(count: Int = 50): Result<List<BibleCharacter>>

    /**
     * Random Memory Cards pairs for a new board. Fetches up to [count]
     * active pairs from `memory_card_pairs`; the ViewModel takes 6 of
     * those to build a 12-card 3×4 grid.
     */
    suspend fun getMemoryCardPairs(count: Int = 30): Result<List<MemoryCardPair>>

    /**
     * Random Verse Scramble verses for a new round. ViewModel takes 5 of
     * these to build a 5-verse round. Caller may pass [maxWordCount] to
     * restrict to short verses tractable on a phone — defaults to 12.
     */
    suspend fun getVerseScrambles(
        count: Int = 25,
        maxWordCount: Int = 12
    ): Result<List<BibleVerseScramble>>

    /**
     * Loads the candidate biblical event pool for Timeline Sorting. The
     * ViewModel picks 5 random events per puzzle (3 puzzles per round).
     */
    suspend fun getBibleEvents(count: Int = 50): Result<List<BibleEvent>>

    /**
     * Records one answered question/passage + updates the user's stats
     * (points, streak, last_played_at) in a single repository call so the
     * UI doesn't have to orchestrate the writes. [isDaily] controls
     * whether the attempt feeds the leaderboard.
     */
    suspend fun recordAttempt(
        mode: GameMode,
        questionId: String? = null,
        passageId: String? = null,
        characterId: String? = null,
        pairId: String? = null,
        scrambleId: String? = null,
        correct: Boolean,
        pointsEarned: Int,
        isDaily: Boolean
    ): Result<Unit>

    /**
     * Called after the user finishes a Daily Challenge round of the given
     * difficulty. Stamps the per-difficulty unlock timestamp and updates
     * the streak. Streak fires when this is the first daily completion of
     * the day (any difficulty counts) — so a player who only ever does
     * Easy still maintains their fire.
     */
    suspend fun completeDailyChallenge(
        difficulty: GameDifficulty
    ): Result<GameStats>

    /**
     * Called after the user finishes the Daily Verse (FITB) round. Stamps
     * `last_daily_fitb_at` and updates streak using the same "first daily
     * of the day across all rounds" rule.
     */
    suspend fun completeDailyFitb(): Result<GameStats>

    /** Per-user stats (own user). Used by the Games hub + Home card. */
    suspend fun getMyStats(): Result<GameStats>

    /**
     * Weekly Top-N for the caller's cell group. Empty list if the user
     * isn't in a group, or if no attempts have happened this week yet.
     * Counts Daily-Challenge points only (Practice doesn't feed it).
     */
    suspend fun getWeeklyLeaderboard(limit: Int = 5): Result<List<LeaderboardEntry>>

    /**
     * Monthly global Top-N across the entire church. Counts BOTH Daily
     * Challenge AND Practice points. Resets automatically on the 1st of
     * each calendar month at 00:00 (server time). No cron job needed —
     * the underlying RPC uses `date_trunc('month', NOW())`.
     */
    suspend fun getMonthlyGlobalLeaderboard(limit: Int = 25): Result<List<LeaderboardEntry>>

    // ---- LIFELINES (v14) -------------------------------------------------

    /** Caller's remaining lifeline counts. Refreshes nightly via server. */
    suspend fun getLifelines(): Result<LifelinesState>

    /** Spends one lifeline of [kind] and returns the new balance. */
    suspend fun useLifeline(kind: LifelineKind): Result<LifelinesState>

    // ---- Curation (leader/admin only — RLS enforced) ---------------------

    /**
     * Every question regardless of is_active. Used by the Manage Questions
     * screen so leaders can revive inactive items. Members never call this
     * — they only see active questions via the daily/practice endpoints.
     */
    suspend fun listAllQuestions(): Result<List<BibleQuestion>>

    /**
     * Insert (when `id` is blank) or update (otherwise). The repository
     * picks which path so callers don't have to track two methods.
     */
    suspend fun saveQuestion(question: BibleQuestion): Result<Unit>

    /** Flip is_active without touching anything else. */
    suspend fun setQuestionActive(id: String, isActive: Boolean): Result<Unit>

    /** All FITB passages regardless of is_active. */
    suspend fun listAllPassages(): Result<List<BiblePassage>>

    /** Insert (when `id` is blank) or update. */
    suspend fun savePassage(passage: BiblePassage): Result<Unit>

    /** Flip is_active on a passage. */
    suspend fun setPassageActive(id: String, isActive: Boolean): Result<Unit>
}
