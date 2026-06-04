package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.remote.supabase.dto.BibleCharacterDto
import com.grace.app.data.remote.supabase.dto.BiblePassageDto
import com.grace.app.data.remote.supabase.dto.BibleEventDto
import com.grace.app.data.remote.supabase.dto.BibleVerseScrambleDto
import com.grace.app.data.remote.supabase.dto.LifelinesDto
import com.grace.app.data.remote.supabase.dto.MemoryCardPairDto
import com.grace.app.data.remote.supabase.dto.BiblePassageInsertDto
import com.grace.app.data.remote.supabase.dto.BibleQuestionDto
import com.grace.app.data.remote.supabase.dto.BibleQuestionInsertDto
import com.grace.app.data.remote.supabase.dto.GameAttemptInsertDto
import com.grace.app.data.remote.supabase.dto.GameUserStatsDto
import com.grace.app.data.remote.supabase.dto.LeaderboardEntryDto
import com.grace.app.data.remote.supabase.dto.MonthlyLeaderboardEntryDto
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.BibleCharacter
import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.model.BibleEvent
import com.grace.app.domain.model.BibleVerseScramble
import com.grace.app.domain.model.LifelineKind
import com.grace.app.domain.model.LifelinesState
import com.grace.app.domain.model.MemoryCardPair
import com.grace.app.domain.model.GameDifficulty
import com.grace.app.domain.model.GameMode
import com.grace.app.domain.model.GameStats
import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.domain.model.QuestionCategory
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class GamesRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo
) : GamesRepository {

    private suspend fun currentUid(): String? =
        supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()

    // ---- QUESTIONS --------------------------------------------------------

    override suspend fun getDailyChallengeQuestions(
        difficulty: GameDifficulty
    ): Result<List<BibleQuestion>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to play today's challenge.")
        }
        return try {
            // Pool filtered to this difficulty; deterministic shuffle
            // keyed by (date, difficulty) so all players see the same
            // 10 questions for, e.g., Easy today.
            val pool = supabase.from("bible_questions")
                .select {
                    filter {
                        eq("is_active", true)
                        eq("difficulty", difficulty.dbValue)
                    }
                }
                .decodeList<BibleQuestionDto>()
                .map { it.toDomain() }
            if (pool.isEmpty()) return Result.Success(emptyList())
            val seed = LocalDate.now().toEpochDay() xor difficulty.ordinal.toLong()
            val picked = pool.shuffled(Random(seed)).take(DAILY_CHALLENGE_SIZE)
            Result.Success(picked)
        } catch (e: Exception) {
            Result.Error("Couldn't load today's challenge.", e)
        }
    }

    override suspend fun getPracticeQuestions(
        category: QuestionCategory?,
        count: Int
    ): Result<List<BibleQuestion>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to play.")
        }
        return try {
            val all = supabase.from("bible_questions")
                .select {
                    filter {
                        eq("is_active", true)
                        if (category != null) eq("category", category.dbValue)
                    }
                }
                .decodeList<BibleQuestionDto>()
                .map { it.toDomain() }
            val pool = all.shuffled().take(count.coerceAtMost(PRACTICE_MAX))
            Result.Success(pool)
        } catch (e: Exception) {
            Result.Error("Couldn't load practice questions.", e)
        }
    }

    override suspend fun getWhoAmICharacters(count: Int): Result<List<BibleCharacter>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to play Who Am I?.")
        }
        return try {
            val all = supabase.from("bible_characters")
                .select { filter { eq("is_active", true) } }
                .decodeList<BibleCharacterDto>()
                .map { it.toDomain() }
            val pool = all.shuffled().take(count.coerceAtMost(WHO_AM_I_MAX))
            Result.Success(pool)
        } catch (e: Exception) {
            Result.Error("Couldn't load Who Am I? characters.", e)
        }
    }

    override suspend fun getBibleEvents(count: Int): Result<List<BibleEvent>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to play Timeline Sorting.")
        }
        return try {
            val all = supabase.from("bible_events")
                .select { filter { eq("is_active", true) } }
                .decodeList<BibleEventDto>()
                .map { it.toDomain() }
            // Don't pre-shuffle here — the ViewModel needs the full pool
            // to pick 5 events with NON-adjacent chronological_order (so
            // puzzles don't degenerate into "guess which order looks right").
            val capped = all.take(count.coerceAtMost(BIBLE_EVENTS_MAX))
            Result.Success(capped)
        } catch (e: Exception) {
            Result.Error("Couldn't load Timeline events.", e)
        }
    }

    override suspend fun getVerseScrambles(
        count: Int,
        maxWordCount: Int
    ): Result<List<BibleVerseScramble>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to play Verse Scramble.")
        }
        return try {
            val all = supabase.from("bible_verse_scrambles")
                .select {
                    filter {
                        eq("is_active", true)
                        lte("word_count", maxWordCount)
                    }
                }
                .decodeList<BibleVerseScrambleDto>()
                .map { it.toDomain() }
            val pool = all.shuffled().take(count.coerceAtMost(VERSE_SCRAMBLES_MAX))
            Result.Success(pool)
        } catch (e: Exception) {
            Result.Error("Couldn't load Verse Scramble verses.", e)
        }
    }

    override suspend fun getMemoryCardPairs(count: Int): Result<List<MemoryCardPair>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to play Memory Cards.")
        }
        return try {
            val all = supabase.from("memory_card_pairs")
                .select { filter { eq("is_active", true) } }
                .decodeList<MemoryCardPairDto>()
                .map { it.toDomain() }
            val pool = all.shuffled().take(count.coerceAtMost(MEMORY_PAIRS_MAX))
            Result.Success(pool)
        } catch (e: Exception) {
            Result.Error("Couldn't load Memory Cards pairs.", e)
        }
    }

    override suspend fun getRandomFillInBlank(): Result<BiblePassage?> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to play.")
        }
        return try {
            val pool = supabase.from("bible_passages")
                .select { filter { eq("is_active", true) } }
                .decodeList<BiblePassageDto>()
                .map { it.toDomain() }
            Result.Success(pool.randomOrNull())
        } catch (e: Exception) {
            Result.Error("Couldn't load a verse.", e)
        }
    }

    // ---- WRITES -----------------------------------------------------------

    override suspend fun recordAttempt(
        mode: GameMode,
        questionId: String?,
        passageId: String?,
        characterId: String?,
        pairId: String?,
        scrambleId: String?,
        correct: Boolean,
        pointsEarned: Int,
        isDaily: Boolean
    ): Result<Unit> {
        val uid = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Your score will save when you reconnect.")
        }
        return try {
            supabase.from("game_attempts").insert(
                GameAttemptInsertDto(
                    userId = uid,
                    mode = mode.dbValue,
                    questionId = questionId,
                    passageId = passageId,
                    characterId = characterId,
                    pairId = pairId,
                    scrambleId = scrambleId,
                    correct = correct,
                    pointsEarned = pointsEarned,
                    isDaily = isDaily
                )
            )
            bumpIncrementalPoints(uid, pointsEarned)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't save your answer.", e)
        }
    }

    override suspend fun completeDailyChallenge(
        difficulty: GameDifficulty
    ): Result<GameStats> {
        val uid = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to finalize today's challenge.")
        }
        return try {
            val existing = supabase.from("game_user_stats")
                .select { filter { eq("user_id", uid) } }
                .decodeSingleOrNull<GameUserStatsDto>()
            val now = OffsetDateTime.now()
            val today = LocalDate.now()

            val newStreak = computeStreak(existing, today)
            val newLongest = maxOf(existing?.longestStreak ?: 0, newStreak)
            val payload = GameUserStatsDto(
                userId = uid,
                currentStreak = newStreak,
                longestStreak = newLongest,
                totalPoints = existing?.totalPoints ?: 0L,
                lastPlayedAt = now.toString(),
                lastEasyAt = if (difficulty == GameDifficulty.EASY)
                    now.toString() else existing?.lastEasyAt,
                lastMediumAt = if (difficulty == GameDifficulty.MEDIUM)
                    now.toString() else existing?.lastMediumAt,
                lastHardAt = if (difficulty == GameDifficulty.HARD)
                    now.toString() else existing?.lastHardAt,
                lastFitbAt = existing?.lastFitbAt
            )
            supabase.from("game_user_stats").upsert(payload)
            Result.Success(payload.toDomain())
        } catch (e: Exception) {
            Result.Error("Couldn't update your streak.", e)
        }
    }

    override suspend fun completeDailyFitb(): Result<GameStats> {
        val uid = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to finalize Daily Verse.")
        }
        return try {
            val existing = supabase.from("game_user_stats")
                .select { filter { eq("user_id", uid) } }
                .decodeSingleOrNull<GameUserStatsDto>()
            val now = OffsetDateTime.now()
            val today = LocalDate.now()
            val newStreak = computeStreak(existing, today)
            val newLongest = maxOf(existing?.longestStreak ?: 0, newStreak)
            val payload = GameUserStatsDto(
                userId = uid,
                currentStreak = newStreak,
                longestStreak = newLongest,
                totalPoints = existing?.totalPoints ?: 0L,
                lastPlayedAt = now.toString(),
                lastEasyAt = existing?.lastEasyAt,
                lastMediumAt = existing?.lastMediumAt,
                lastHardAt = existing?.lastHardAt,
                lastFitbAt = now.toString()
            )
            supabase.from("game_user_stats").upsert(payload)
            Result.Success(payload.toDomain())
        } catch (e: Exception) {
            Result.Error("Couldn't update your streak.", e)
        }
    }

    /**
     * Streak rule shared by every daily completion path:
     *   - First-ever daily         → streak = 1
     *   - Already completed today  → unchanged (whichever round you did
     *     first today bumped the streak; later rounds today don't double-count)
     *   - Most-recent daily was yesterday → streak + 1
     *   - Older                    → streak resets to 1
     */
    private fun computeStreak(existing: GameUserStatsDto?, today: LocalDate): Int {
        val mostRecentDaily = listOfNotNull(
            existing?.lastEasyAt,
            existing?.lastMediumAt,
            existing?.lastHardAt,
            existing?.lastFitbAt
        ).mapNotNull { parseDateLocal(it) }.maxOrNull()
        return when {
            mostRecentDaily == null -> 1
            mostRecentDaily.isEqual(today) ->
                (existing?.currentStreak ?: 0).coerceAtLeast(1)
            mostRecentDaily.isEqual(today.minusDays(1)) ->
                (existing?.currentStreak ?: 0) + 1
            else -> 1
        }
    }

    // ---- READS ------------------------------------------------------------

    override suspend fun getMyStats(): Result<GameStats> {
        val uid = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Success(GameStats(userId = uid))
        }
        return try {
            val dto = supabase.from("game_user_stats")
                .select { filter { eq("user_id", uid) } }
                .decodeSingleOrNull<GameUserStatsDto>()
            // The hub's big "points" stat now reports this month's sum, in
            // sync with the monthly global leaderboard. We compute on the
            // server (get_my_month_points RPC) so the month boundary uses
            // the same date_trunc the leaderboard uses — no client TZ skew.
            // game_user_stats.total_points stays as lifetime cumulative in
            // the DB (handy for future "all-time best" badges).
            val monthPoints = runCatching {
                supabase.pluginManager.getPlugin(Postgrest)
                    .rpc(function = "get_my_month_points")
                    .data
                    .trim()
                    .toIntOrNull() ?: 0
            }.getOrDefault(0)
            val base = dto?.toDomain() ?: GameStats(userId = uid)
            Result.Success(base.copy(totalPoints = monthPoints.toLong()))
        } catch (e: Exception) {
            Result.Error("Couldn't load your game stats.", e)
        }
    }

    override suspend fun getWeeklyLeaderboard(limit: Int): Result<List<LeaderboardEntry>> {
        val uid = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to see this week's leaderboard.")
        }
        val groupId = prefs.groupId.first()
        if (groupId.isNullOrBlank()) {
            return Result.Success(emptyList())
        }
        return try {
            // SECURITY DEFINER RPC instead of selecting the view directly.
            // game_attempts' RLS hides other users' rows from regular
            // members, which would break the leaderboard; the RPC reveals
            // ONLY the aggregated row-per-user (no individual attempt
            // leakage). Server-side group/role check inside the function.
            val rows = supabase
                .pluginManager
                .getPlugin(Postgrest)
                .rpc(
                    function = "get_weekly_group_leaderboard",
                    parameters = LeaderboardRpcParams(
                        pGroupId = groupId, pLimit = limit
                    )
                )
                .decodeList<LeaderboardEntryDto>()
                .map { it.toDomain(currentUserId = uid) }
            Result.Success(rows)
        } catch (e: Exception) {
            Result.Error("Couldn't load the leaderboard.", e)
        }
    }

    @Serializable
    private data class LeaderboardRpcParams(
        @SerialName("p_group_id") val pGroupId: String,
        @SerialName("p_limit") val pLimit: Int
    )

    override suspend fun getMonthlyGlobalLeaderboard(
        limit: Int
    ): Result<List<LeaderboardEntry>> {
        val uid = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to see the global leaderboard.")
        }
        return try {
            // Same SECURITY DEFINER pattern as weekly — the RPC aggregates
            // over the entire game_attempts table (would be blocked by RLS
            // for any single SELECT). The function authenticates internally
            // so anonymous callers cannot invoke it.
            val rows = supabase
                .pluginManager
                .getPlugin(Postgrest)
                .rpc(
                    function = "get_monthly_global_leaderboard",
                    parameters = MonthlyLeaderboardRpcParams(pLimit = limit)
                )
                .decodeList<MonthlyLeaderboardEntryDto>()
                .map { it.toDomain(currentUserId = uid) }
            Result.Success(rows)
        } catch (e: Exception) {
            Result.Error("Couldn't load the global leaderboard.", e)
        }
    }

    @Serializable
    private data class MonthlyLeaderboardRpcParams(
        @SerialName("p_limit") val pLimit: Int
    )

    // ---- LIFELINES (v14) -------------------------------------------------

    override suspend fun getLifelines(): Result<LifelinesState> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline.")
        }
        return try {
            // RPC returns a single-row table — decode as a list and take
            // the first row. Server auto-refills on read if stale.
            val rows = supabase.pluginManager.getPlugin(Postgrest)
                .rpc(function = "get_lifelines")
                .decodeList<LifelinesDto>()
            val first = rows.firstOrNull() ?: LifelinesDto(0, 0)
            Result.Success(
                LifelinesState(
                    joshuaRemaining = first.joshua,
                    danielRemaining = first.daniel
                )
            )
        } catch (e: Exception) {
            Result.Error("Couldn't load your lifelines.", e)
        }
    }

    override suspend fun useLifeline(kind: LifelineKind): Result<LifelinesState> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline.")
        }
        return try {
            val rows = supabase.pluginManager.getPlugin(Postgrest)
                .rpc(
                    function = "use_lifeline",
                    parameters = UseLifelineRpcParams(kind = kind.dbValue)
                )
                .decodeList<LifelinesDto>()
            val first = rows.firstOrNull() ?: LifelinesDto(0, 0)
            Result.Success(
                LifelinesState(
                    joshuaRemaining = first.joshua,
                    danielRemaining = first.daniel
                )
            )
        } catch (e: Exception) {
            // The RPC raises if the user has none left — message reaches
            // here via the exception. Pass it along so the UI can show it.
            val msg = e.message ?: "Couldn't use that lifeline."
            Result.Error(
                when {
                    "No Joshua" in msg -> "No Joshua Effects left today."
                    "No Daniel" in msg -> "No Daniel Effects left today."
                    else -> "Couldn't use that lifeline."
                },
                e
            )
        }
    }

    @Serializable
    private data class UseLifelineRpcParams(
        @SerialName("kind") val kind: String
    )

    // ---- CURATION (leader/admin only — RLS enforced) ---------------------

    override suspend fun listAllQuestions(): Result<List<BibleQuestion>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to manage questions.")
        }
        return try {
            val rows = supabase.from("bible_questions")
                .select()
                .decodeList<BibleQuestionDto>()
                .map { it.toDomain() }
                .sortedWith(
                    compareByDescending<BibleQuestion> { it.isActive }
                        .thenBy { it.difficulty.ordinal }
                        .thenBy { it.question }
                )
            Result.Success(rows)
        } catch (e: Exception) {
            Result.Error("Couldn't load questions.", e)
        }
    }

    override suspend fun saveQuestion(question: BibleQuestion): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to save.")
        }
        return try {
            if (question.id.isBlank()) {
                supabase.from("bible_questions").insert(
                    BibleQuestionInsertDto(
                        category = question.category.dbValue,
                        difficulty = question.difficulty.dbValue,
                        question = question.question,
                        options = question.options,
                        correctIndex = question.correctIndex,
                        explanation = question.explanation,
                        sourceRef = question.sourceRef
                    )
                )
            } else {
                supabase.from("bible_questions").update({
                    set("category", question.category.dbValue)
                    set("difficulty", question.difficulty.dbValue)
                    set("question", question.question)
                    set("options", question.options)
                    set("correct_index", question.correctIndex)
                    set("explanation", question.explanation)
                    set("source_ref", question.sourceRef)
                    set("is_active", question.isActive)
                }) {
                    filter { eq("id", question.id) }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(crudErrorMsg(e), e)
        }
    }

    override suspend fun setQuestionActive(
        id: String, isActive: Boolean
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to update.")
        }
        return try {
            supabase.from("bible_questions").update({
                set("is_active", isActive)
            }) { filter { eq("id", id) } }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(crudErrorMsg(e), e)
        }
    }

    override suspend fun listAllPassages(): Result<List<BiblePassage>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to manage verses.")
        }
        return try {
            val rows = supabase.from("bible_passages")
                .select()
                .decodeList<BiblePassageDto>()
                .map { it.toDomain() }
                .sortedWith(
                    compareByDescending<BiblePassage> { it.isActive }
                        .thenBy { it.reference }
                )
            Result.Success(rows)
        } catch (e: Exception) {
            Result.Error("Couldn't load verses.", e)
        }
    }

    override suspend fun savePassage(passage: BiblePassage): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to save.")
        }
        return try {
            if (passage.id.isBlank()) {
                supabase.from("bible_passages").insert(
                    BiblePassageInsertDto(
                        reference = passage.reference,
                        text = passage.text,
                        blankWord = passage.blankWord,
                        distractors = passage.distractors
                    )
                )
            } else {
                supabase.from("bible_passages").update({
                    set("reference", passage.reference)
                    set("text", passage.text)
                    set("blank_word", passage.blankWord)
                    set("distractors", passage.distractors)
                    set("is_active", passage.isActive)
                }) {
                    filter { eq("id", passage.id) }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(crudErrorMsg(e), e)
        }
    }

    override suspend fun setPassageActive(
        id: String, isActive: Boolean
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to update.")
        }
        return try {
            supabase.from("bible_passages").update({
                set("is_active", isActive)
            }) { filter { eq("id", id) } }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(crudErrorMsg(e), e)
        }
    }

    private fun crudErrorMsg(e: Exception): String = when {
        e.message?.contains("row-level security", ignoreCase = true) == true ->
            "You don't have permission. Leader role required."
        else -> "Couldn't save changes. Try again."
    }

    // ---- helpers ----------------------------------------------------------

    /** Patch total_points + last_played_at on every attempt. Idempotent. */
    private suspend fun bumpIncrementalPoints(uid: String, delta: Int) {
        runCatching {
            val existing = supabase.from("game_user_stats")
                .select { filter { eq("user_id", uid) } }
                .decodeSingleOrNull<GameUserStatsDto>()
            val payload = GameUserStatsDto(
                userId = uid,
                currentStreak = existing?.currentStreak ?: 0,
                longestStreak = existing?.longestStreak ?: 0,
                totalPoints = (existing?.totalPoints ?: 0L) + delta,
                lastPlayedAt = OffsetDateTime.now().toString(),
                lastEasyAt = existing?.lastEasyAt,
                lastMediumAt = existing?.lastMediumAt,
                lastHardAt = existing?.lastHardAt,
                lastFitbAt = existing?.lastFitbAt
            )
            supabase.from("game_user_stats").upsert(payload)
        }
    }

    private fun parseDateLocal(iso: String): LocalDate? = runCatching {
        OffsetDateTime.parse(iso)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDate()
    }.getOrNull()

    @Suppress("unused") private val keepDateTimeImport: LocalDateTime? = null

    private companion object {
        const val DAILY_CHALLENGE_SIZE = 10
        // Practice fetches the entire active pool now (~300 questions after
        // the v6 bulk seed). The ViewModel handles in-session shuffling and
        // wrap-around so a session goes through every question once before
        // any repeat.
        const val PRACTICE_MAX = 500
        // Same pattern for Who Am I? — fetch the entire pool (currently ~30)
        // and let the ViewModel walk the shuffled list.
        const val WHO_AM_I_MAX = 200
        // Memory Cards pool cap. We pull all active pairs (~30 in seed),
        // and the ViewModel picks 6 random ones for each board.
        const val MEMORY_PAIRS_MAX = 200
        // Verse Scramble pool cap — ViewModel picks 5 verses per round.
        const val VERSE_SCRAMBLES_MAX = 200
        // Timeline Sorting events pool cap — ViewModel picks 5 per puzzle.
        const val BIBLE_EVENTS_MAX = 200
    }
}
