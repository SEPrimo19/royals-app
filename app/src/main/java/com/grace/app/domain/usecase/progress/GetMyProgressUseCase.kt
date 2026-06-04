package com.grace.app.domain.usecase.progress

import com.grace.app.domain.model.PrayerStatus
import com.grace.app.domain.model.ProgressSnapshot
import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Aggregates personal progress across four repos into one snapshot.
 *
 * Each contributing fetch is wrapped in `runCatching` so a single broken
 * source (network blip, RLS issue) doesn't blank the whole screen — the
 * field just stays at its default 0. The fetches don't depend on each
 * other, but they're sequential here for code clarity; the screen would
 * see no measurable benefit from parallelizing them in this case.
 */
class GetMyProgressUseCase @Inject constructor(
    private val devotionalRepository: DevotionalRepository,
    private val prayerRepository: PrayerRepository,
    private val feedRepository: FeedRepository,
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(): ProgressSnapshot {
        val streak = runCatching { devotionalRepository.getStreak().first() }
            .getOrDefault(0)

        val devotionalsCompleted = runCatching {
            devotionalRepository.getCompletedCount().first()
        }.getOrDefault(0)

        val myPrayers = runCatching {
            (prayerRepository.getMyPrayers() as? Result.Success)?.data.orEmpty()
        }.getOrDefault(emptyList())
        val prayersPosted = myPrayers.size
        val prayersAnswered = myPrayers.count { it.status == PrayerStatus.ANSWERED }

        val prayersInterceded = runCatching {
            (prayerRepository.getMyIntercessions() as? Result.Success)?.data?.size ?: 0
        }.getOrDefault(0)

        val postsShared = runCatching {
            (feedRepository.getMyPosts() as? Result.Success)?.data?.size ?: 0
        }.getOrDefault(0)

        val eventsAttended = runCatching {
            (eventRepository.getMyAttendance() as? Result.Success)?.data?.size ?: 0
        }.getOrDefault(0)

        return ProgressSnapshot(
            devoStreak = streak,
            devotionalsCompleted = devotionalsCompleted,
            prayersPosted = prayersPosted,
            prayersAnswered = prayersAnswered,
            prayersInterceded = prayersInterceded,
            postsShared = postsShared,
            eventsAttended = eventsAttended
        )
    }
}
