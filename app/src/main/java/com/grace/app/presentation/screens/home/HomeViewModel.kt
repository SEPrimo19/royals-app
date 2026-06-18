package com.grace.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.UserDevoProgressDao
import com.grace.app.data.local.dao.VerseDao
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.Devotional
import com.grace.app.domain.model.Post
import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.User
import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.usecase.feed.GetFeedPostsUseCase
import com.grace.app.domain.usecase.leader.GetMyLeaderUseCase
import com.grace.app.domain.usecase.prayer.GetPrayersUseCase
import com.grace.app.domain.usecase.devotional.GetTodayDevotionalUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val greeting: String = "Welcome",
    val streak: Int = 0,
    val gameStreak: Int = 0,
    val todayDevotional: Devotional? = null,
    val devoDone: Boolean = false,
    val recentPrayers: List<Prayer> = emptyList(),
    val spotlightPost: Post? = null,
    val myLeader: User? = null,
    val offlineVerseText: String? = null,
    val offlineVerseRef: String? = null,
    val isOnline: Boolean = true,
    val justCameOnline: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayDevotionalUseCase: GetTodayDevotionalUseCase,
    private val devotionalRepository: DevotionalRepository,
    private val getPrayersUseCase: GetPrayersUseCase,
    private val getFeedPostsUseCase: GetFeedPostsUseCase,
    private val getMyLeaderUseCase: GetMyLeaderUseCase,
    private val getMyGameStatsUseCase:
        com.grace.app.domain.usecase.games.GetMyGameStatsUseCase,
    private val prefs: UserPreferencesRepo,
    private val verseDao: VerseDao,
    private val progressDao: UserDevoProgressDao,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(greeting = greetingForNow()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val serverFetchJobs = mutableListOf<Job>()

    private var onlinePillJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.userName.collect { name ->
                _uiState.update { it.copy(userName = name ?: "") }
            }
        }
        viewModelScope.launch {
            devotionalRepository.getStreak().collect { s ->
                _uiState.update { it.copy(streak = s) }
            }
        }
        viewModelScope.launch {
            var wasOnline: Boolean? = null
            networkMonitor.networkState.collect { online ->
                val cameBackOnline = wasOnline == false && online
                wasOnline = online
                _uiState.update {
                    it.copy(
                        isOnline = online,
                        justCameOnline = if (cameBackOnline) true
                            else it.justCameOnline && online
                    )
                }
                if (cameBackOnline) {
                    loadServerData()
                    onlinePillJob?.cancel()
                    onlinePillJob = viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _uiState.update { it.copy(justCameOnline = false) }
                    }
                }
            }
        }
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            combine(
                prefs.userId,
                _uiState.asStateFlow()
            ) { uid, state -> uid to state.todayDevotional?.id }
                .distinctUntilChanged()
                .flatMapLatest { (uid, devoId) ->
                    if (uid.isNullOrBlank() || devoId.isNullOrBlank()) flowOf(null)
                    else progressDao.observeProgress(uid, devoId)
                }
                .collect { progress ->
                    _uiState.update { it.copy(devoDone = progress != null) }
                }
        }
        loadServerData()
    }

    fun refresh() = loadServerData()

    private fun loadServerData() {
        serverFetchJobs.forEach { it.cancel() }
        serverFetchJobs.clear()
        serverFetchJobs += viewModelScope.launch {
            getTodayDevotionalUseCase().collect { r ->
                if (r is Result.Success) {
                    _uiState.update { it.copy(todayDevotional = r.data) }
                }
            }
        }
        serverFetchJobs += viewModelScope.launch {
            getPrayersUseCase(null).collect { r ->
                if (r is Result.Success) {
                    _uiState.update { it.copy(recentPrayers = r.data.take(3)) }
                }
            }
        }
        serverFetchJobs += viewModelScope.launch {
            val r = getMyGameStatsUseCase()
            if (r is Result.Success) {
                _uiState.update { it.copy(gameStreak = r.data.currentStreak) }
            }
        }
        serverFetchJobs += viewModelScope.launch {
            getFeedPostsUseCase().collect { r ->
                if (r is Result.Success) {
                    _uiState.update {
                        it.copy(spotlightPost = r.data.firstOrNull { p -> p.isHighlighted })
                    }
                }
            }
        }
        serverFetchJobs += viewModelScope.launch {
            getMyLeaderUseCase().collect { r ->
                if (r is Result.Success) _uiState.update { it.copy(myLeader = r.data) }
            }
        }
        serverFetchJobs += viewModelScope.launch {
            val verses = verseDao.getAll().first()
            verses.randomOrNull()?.let { v ->
                _uiState.update {
                    it.copy(offlineVerseText = v.text, offlineVerseRef = v.ref)
                }
            }
        }
    }

    private fun greetingForNow(): String {
        val h = LocalTime.now().hour
        return when {
            h < 12 -> "Good morning,"
            h < 17 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }
}
