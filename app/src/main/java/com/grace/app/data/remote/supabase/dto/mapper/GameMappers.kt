package com.grace.app.data.remote.supabase.dto.mapper

import com.grace.app.data.remote.supabase.dto.BibleCharacterDto
import com.grace.app.data.remote.supabase.dto.BibleEventDto
import com.grace.app.data.remote.supabase.dto.BiblePassageDto
import com.grace.app.data.remote.supabase.dto.BibleQuestionDto
import com.grace.app.data.remote.supabase.dto.BibleVerseScrambleDto
import com.grace.app.data.remote.supabase.dto.GameUserStatsDto
import com.grace.app.data.remote.supabase.dto.LeaderboardEntryDto
import com.grace.app.data.remote.supabase.dto.MemoryCardPairDto
import com.grace.app.data.remote.supabase.dto.MonthlyLeaderboardEntryDto
import com.grace.app.domain.model.BibleCharacter
import com.grace.app.domain.model.BibleEvent
import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.model.BibleVerseScramble
import com.grace.app.domain.model.MemoryCardPair
import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.model.GameDifficulty
import com.grace.app.domain.model.GameStats
import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.domain.model.QuestionCategory

fun BibleQuestionDto.toDomain(): BibleQuestion = BibleQuestion(
    id = id,
    category = QuestionCategory.fromDb(category),
    difficulty = GameDifficulty.fromDb(difficulty),
    question = question,
    options = options,
    correctIndex = correctIndex,
    explanation = explanation,
    sourceRef = sourceRef,
    isActive = isActive
)

fun BibleCharacterDto.toDomain(): BibleCharacter = BibleCharacter(
    id = id,
    name = name,
    category = QuestionCategory.fromDb(category),
    difficulty = GameDifficulty.fromDb(difficulty),
    clues = listOf(clue1, clue2, clue3, clue4),
    distractors = distractors,
    sourceRef = sourceRef,
    explanation = explanation,
    isActive = isActive
)

fun BibleEventDto.toDomain(): BibleEvent = BibleEvent(
    id = id,
    title = title,
    description = description,
    chronologicalOrder = chronologicalOrder,
    approxYearText = approxYearText,
    sourceRef = sourceRef,
    isActive = isActive
)

fun BibleVerseScrambleDto.toDomain(): BibleVerseScramble = BibleVerseScramble(
    id = id,
    reference = reference,
    text = text,
    wordCount = wordCount,
    isActive = isActive
)

fun MemoryCardPairDto.toDomain(): MemoryCardPair = MemoryCardPair(
    id = id,
    reference = reference,
    verseSnippet = verseSnippet,
    fullText = fullText,
    isActive = isActive
)

fun BiblePassageDto.toDomain(): BiblePassage = BiblePassage(
    id = id,
    reference = reference,
    text = text,
    blankWord = blankWord,
    distractors = distractors,
    isActive = isActive
)

fun GameUserStatsDto.toDomain(): GameStats = GameStats(
    userId = userId,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    totalPoints = totalPoints,
    lastPlayedAt = lastPlayedAt?.let { parseDateTime(it) },
    lastEasyAt = lastEasyAt?.let { parseDateTime(it) },
    lastMediumAt = lastMediumAt?.let { parseDateTime(it) },
    lastHardAt = lastHardAt?.let { parseDateTime(it) },
    lastFitbAt = lastFitbAt?.let { parseDateTime(it) }
)

fun LeaderboardEntryDto.toDomain(currentUserId: String?): LeaderboardEntry =
    LeaderboardEntry(
        userId = userId,
        userName = userName,
        groupId = groupId,
        groupName = null,  // weekly board is always within-group; chip not needed
        points = weekPoints,
        attempts = weekAttempts,
        isMe = userId == currentUserId
    )

fun MonthlyLeaderboardEntryDto.toDomain(currentUserId: String?): LeaderboardEntry =
    LeaderboardEntry(
        userId = userId,
        userName = userName,
        groupId = groupId,
        groupName = groupName,
        points = monthPoints,
        attempts = monthAttempts,
        isMe = userId == currentUserId
    )
