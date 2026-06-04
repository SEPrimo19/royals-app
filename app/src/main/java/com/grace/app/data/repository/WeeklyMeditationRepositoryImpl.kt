package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.MeditationSubmissionDao
import com.grace.app.data.local.dao.WeeklyMeditationDao
import com.grace.app.data.remote.supabase.dto.MeditationSubmissionDto
import com.grace.app.data.remote.supabase.dto.MeditationSubmissionInsertDto
import com.grace.app.data.remote.supabase.dto.WeeklyMeditationDto
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.remote.supabase.dto.mapper.toEntity
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.repository.WeeklyMeditationRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyMeditationRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val meditationDao: WeeklyMeditationDao,
    private val submissionDao: MeditationSubmissionDao,
    private val prefs: UserPreferencesRepo,
    private val networkMonitor: NetworkMonitor
) : WeeklyMeditationRepository {

    // Long-lived scope for fire-and-forget background refreshes. Singleton
    // so it survives ViewModel lifecycle without leaking — the JVM owns it.
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---- public API --------------------------------------------------------

    override fun observeCurrentMeditation(): Flow<WeeklyMeditation?> = flow {
        val today = LocalDate.now().toString()
        // Emit Room first so the UI gets something instantly even offline.
        meditationDao.observeCurrent(today)
            .map { it?.toDomain() }
            .distinctUntilChanged()
            .onStart {
                // Kick a background refresh on first collection (offline-safe).
                refreshScope.launch { refreshMeditationsFromServer() }
            }
            .collect { emit(it) }
    }

    override fun observeAllMeditations(): Flow<List<WeeklyMeditation>> =
        meditationDao.observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .onStart { refreshScope.launch { refreshMeditationsFromServer() } }

    override suspend fun getMeditationById(id: String): WeeklyMeditation? =
        meditationDao.getById(id)?.toDomain()

    override fun observeMySubmissions(): Flow<List<MeditationSubmission>> = flow {
        val uid = prefs.userId.first().orEmpty()
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        submissionDao.observeMine(uid)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .onStart {
                refreshScope.launch { refreshMySubmissionsFromServer(uid) }
            }
            .collect { emit(it) }
    }

    override suspend fun findMySubmission(meditationId: String):
        MeditationSubmission? {
        val uid = prefs.userId.first().orEmpty().ifBlank { return null }
        // Refresh first when online so a freshly-submitted reflection on
        // another device shows up immediately when the user revisits.
        if (networkMonitor.isOnline) {
            runCatching { refreshMySubmissionsFromServer(uid) }
        }
        return submissionDao.findMy(uid, meditationId)?.toDomain()
    }

    override suspend fun submitReflection(
        meditationId: String,
        reflectionText: String
    ): Result<Unit> {
        val uid = prefs.userId.first().orEmpty()
            .ifBlank { return Result.Error("Your session expired. Please sign in again.") }
        val trimmed = reflectionText.trim()
        if (trimmed.isEmpty()) return Result.Error("Write a reflection before saving.")
        if (!networkMonitor.isOnline) {
            return Result.Error(
                "You're offline. Reflections need a connection so your leader can see them."
            )
        }
        return try {
            // Upsert returns the canonical row. The UNIQUE (user_id,
            // meditation_id) constraint + `onConflict = "user_id,meditation_id"`
            // means an existing submission is updated in place, not duplicated.
            val saved = supabase.from("user_meditation_submissions")
                .upsert(
                    value = MeditationSubmissionInsertDto(
                        userId = uid,
                        meditationId = meditationId,
                        reflectionText = trimmed
                    ),
                    onConflict = "user_id,meditation_id"
                ) {
                    select(Columns.ALL)
                }
                .decodeSingle<MeditationSubmissionDto>()
            submissionDao.upsert(saved.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.Error("Couldn't save your reflection. Try again.", e)
        }
    }

    override suspend fun getSubmissionsForUser(
        userId: String
    ): Result<List<MeditationSubmission>> = try {
        // RLS filters at the DB layer — we just ask. Empty list is a valid
        // "you don't have access" response (no error to surface).
        val rows = supabase.from("user_meditation_submissions")
            .select { filter { eq("user_id", userId) } }
            .decodeList<MeditationSubmissionDto>()
            .map { it.toDomain() }
            .sortedByDescending { it.submittedAt }
        Result.Success(rows)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Result.Error("Couldn't load reflections.", e)
    }

    // ---- internals ---------------------------------------------------------

    private suspend fun refreshMeditationsFromServer() {
        if (!networkMonitor.isOnline) return
        runCatching {
            val remote = supabase.from("weekly_meditations")
                .select { filter { eq("is_active", true) } }
                .decodeList<WeeklyMeditationDto>()
            meditationDao.upsertAll(remote.map { it.toEntity() })
        }
    }

    private suspend fun refreshMySubmissionsFromServer(uid: String) {
        if (!networkMonitor.isOnline) return
        runCatching {
            val remote = supabase.from("user_meditation_submissions")
                .select { filter { eq("user_id", uid) } }
                .decodeList<MeditationSubmissionDto>()
            submissionDao.upsertAll(remote.map { it.toEntity() })
        }
    }
}
