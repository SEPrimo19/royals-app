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

interface GamesRepository {

    suspend fun getDailyChallengeQuestions(
        difficulty: GameDifficulty
    ): Result<List<BibleQuestion>>

    suspend fun getPracticeQuestions(
        category: QuestionCategory? = null,
        count: Int = 40
    ): Result<List<BibleQuestion>>

    suspend fun getRandomFillInBlank(): Result<BiblePassage?>

    suspend fun getWhoAmICharacters(count: Int = 50): Result<List<BibleCharacter>>

    suspend fun getMemoryCardPairs(count: Int = 30): Result<List<MemoryCardPair>>

    suspend fun getVerseScrambles(
        count: Int = 25,
        maxWordCount: Int = 12
    ): Result<List<BibleVerseScramble>>

    suspend fun getBibleEvents(count: Int = 50): Result<List<BibleEvent>>

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

    suspend fun completeDailyChallenge(
        difficulty: GameDifficulty
    ): Result<GameStats>

    suspend fun completeDailyFitb(): Result<GameStats>

    suspend fun getMyStats(): Result<GameStats>

    suspend fun getWeeklyLeaderboard(limit: Int = 5): Result<List<LeaderboardEntry>>

    suspend fun getMonthlyGlobalLeaderboard(limit: Int = 25): Result<List<LeaderboardEntry>>

    suspend fun getTeamLeaderboard(limit: Int = 25): Result<List<LeaderboardEntry>>


    suspend fun getLifelines(): Result<LifelinesState>

    suspend fun useLifeline(kind: LifelineKind): Result<LifelinesState>


    suspend fun listAllQuestions(): Result<List<BibleQuestion>>

    suspend fun saveQuestion(question: BibleQuestion): Result<Unit>

    suspend fun setQuestionActive(id: String, isActive: Boolean): Result<Unit>

    suspend fun listAllPassages(): Result<List<BiblePassage>>

    suspend fun savePassage(passage: BiblePassage): Result<Unit>

    suspend fun setPassageActive(id: String, isActive: Boolean): Result<Unit>
}
