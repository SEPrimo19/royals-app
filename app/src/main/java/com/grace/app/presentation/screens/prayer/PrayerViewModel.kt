package com.grace.app.presentation.screens.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.model.PrayerStatus
import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.usecase.prayer.GetPrayersUseCase
import com.grace.app.domain.usecase.prayer.IntercedeForPrayerUseCase
import com.grace.app.domain.usecase.prayer.MarkPrayerAnsweredUseCase
import com.grace.app.domain.usecase.prayer.PostPrayerUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PrayerSort { NEWEST, OLDEST, ANSWERED, MOST_PRAYED }

data class PrayerUiState(
    val prayers: List<Prayer> = emptyList(),
    val totalActiveCount: Int = 0,
    val isLoading: Boolean = true,
    val activeFilter: PrayerCategory? = null,
    val sort: PrayerSort = PrayerSort.NEWEST,
    val showPostForm: Boolean = false,
    val newPrayerText: String = "",
    val newPrayerCategory: PrayerCategory = PrayerCategory.PERSONAL,
    val isNewPrayerAnonymous: Boolean = true,
    val isSubmittingPrayer: Boolean = false,
    val prayCountUpdates: Map<String, Int> = emptyMap(),
    val myIntercessions: Set<String> = emptySet(),
    val currentUserId: String? = null,
    val error: String? = null
)

sealed interface PrayerEvent {
    data class FilterChanged(val category: PrayerCategory?) : PrayerEvent
    data class SortChanged(val sort: PrayerSort) : PrayerEvent
    data object TogglePostForm : PrayerEvent
    data class PrayerTextChanged(val text: String) : PrayerEvent
    data class CategorySelected(val category: PrayerCategory) : PrayerEvent
    data object AnonymousToggled : PrayerEvent
    data object SubmitPrayer : PrayerEvent
    data class Intercede(val prayerId: String) : PrayerEvent
    data class MarkAnswered(val prayerId: String) : PrayerEvent
    data object DismissError : PrayerEvent
}

sealed interface PrayerEffect {
    data object PrayerSubmitted : PrayerEffect
    data class ShowError(val message: String) : PrayerEffect
}

@HiltViewModel
class PrayerViewModel @Inject constructor(
    private val getPrayersUseCase: GetPrayersUseCase,
    private val postPrayerUseCase: PostPrayerUseCase,
    private val intercedeForPrayerUseCase: IntercedeForPrayerUseCase,
    private val markPrayerAnsweredUseCase: MarkPrayerAnsweredUseCase,
    private val prayerRepository: PrayerRepository,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PrayerEffect>()
    val effect: SharedFlow<PrayerEffect> = _effect.asSharedFlow()

    private var prayersJob: Job? = null
    private val prayCountJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(currentUserId = prefs.userId.first()) }
        }
        observePrayers(null, PrayerSort.NEWEST)
        loadMyIntercessions()
    }

    fun refresh() {
        val s = _uiState.value
        observePrayers(s.activeFilter, s.sort)
        loadMyIntercessions()
    }

    private fun loadMyIntercessions() {
        viewModelScope.launch {
            (prayerRepository.getMyIntercessions() as? Result.Success)?.let { r ->
                _uiState.update { it.copy(myIntercessions = r.data) }
            }
        }
    }

    fun surfaceError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    private fun observePrayers(category: PrayerCategory?, sort: PrayerSort) {
        prayersJob?.cancel()
        prayersJob = viewModelScope.launch {
            getPrayersUseCase(null).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val all = result.data
                        val byCategory = if (category == null) all
                            else all.filter { it.category == category }
                        val visible = applySort(byCategory, sort)
                        _uiState.update {
                            it.copy(
                                prayers = visible,
                                totalActiveCount = all.size,
                                isLoading = false,
                                error = null
                            )
                        }
                        syncPrayCountSubscriptions(visible.map { p -> p.id })
                    }
                    is Result.Error ->
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    Result.Loading ->
                        _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun applySort(prayers: List<Prayer>, sort: PrayerSort): List<Prayer> {
        return when (sort) {
            PrayerSort.NEWEST -> prayers.sortedByDescending { it.createdAt }
            PrayerSort.OLDEST -> prayers.sortedBy { it.createdAt }
            PrayerSort.ANSWERED -> prayers
                .filter { it.status == PrayerStatus.ANSWERED }
                .sortedByDescending { it.createdAt }
            PrayerSort.MOST_PRAYED -> prayers.sortedByDescending { p ->
                maxOf(
                    p.prayCount,
                    _uiState.value.prayCountUpdates[p.id] ?: 0
                )
            }
        }
    }

    private fun syncPrayCountSubscriptions(visibleIds: List<String>) {
        val gone = prayCountJobs.keys - visibleIds.toSet()
        gone.forEach { id -> prayCountJobs.remove(id)?.cancel() }
        visibleIds.filter { it !in prayCountJobs }.forEach { id ->
            prayCountJobs[id] = viewModelScope.launch {
                prayerRepository.subscribeToPrayCount(id).collect { count ->
                    _uiState.update {
                        it.copy(prayCountUpdates = it.prayCountUpdates + (id to count))
                    }
                }
            }
        }
    }

    fun onEvent(event: PrayerEvent) {
        when (event) {
            is PrayerEvent.FilterChanged -> {
                _uiState.update { it.copy(activeFilter = event.category, isLoading = true) }
                observePrayers(event.category, _uiState.value.sort)
            }
            is PrayerEvent.SortChanged -> {
                _uiState.update { it.copy(sort = event.sort, isLoading = true) }
                observePrayers(_uiState.value.activeFilter, event.sort)
            }
            PrayerEvent.TogglePostForm ->
                _uiState.update { it.copy(showPostForm = !it.showPostForm) }
            is PrayerEvent.PrayerTextChanged ->
                _uiState.update { it.copy(newPrayerText = event.text) }
            is PrayerEvent.CategorySelected ->
                _uiState.update { it.copy(newPrayerCategory = event.category) }
            PrayerEvent.AnonymousToggled ->
                _uiState.update { it.copy(isNewPrayerAnonymous = !it.isNewPrayerAnonymous) }
            PrayerEvent.SubmitPrayer -> submitPrayer()
            is PrayerEvent.Intercede -> intercede(event.prayerId)
            is PrayerEvent.MarkAnswered -> markAnswered(event.prayerId)
            PrayerEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun submitPrayer() {
        var claimed = false
        _uiState.update { current ->
            if (current.isSubmittingPrayer || current.newPrayerText.isBlank()) current
            else {
                claimed = true
                current.copy(isSubmittingPrayer = true, error = null)
            }
        }
        if (!claimed) return
        val s = _uiState.value
        viewModelScope.launch {
            when (val result = postPrayerUseCase(
                s.newPrayerText, s.isNewPrayerAnonymous, s.newPrayerCategory
            )) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingPrayer = false,
                            showPostForm = false,
                            newPrayerText = ""
                        )
                    }
                    _effect.emit(PrayerEffect.PrayerSubmitted)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSubmittingPrayer = false, error = result.message)
                    }
                    _effect.emit(PrayerEffect.ShowError(result.message))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun intercede(prayerId: String) {
        _uiState.update { it.copy(myIntercessions = it.myIntercessions + prayerId) }
        viewModelScope.launch {
            when (val r = intercedeForPrayerUseCase(prayerId)) {
                is Result.Error -> {
                    _uiState.update {
                        it.copy(myIntercessions = it.myIntercessions - prayerId)
                    }
                    _effect.emit(PrayerEffect.ShowError(r.message))
                }
                else -> Unit
            }
        }
    }

    private fun markAnswered(prayerId: String) {
        viewModelScope.launch {
            (markPrayerAnsweredUseCase(prayerId) as? Result.Error)?.let {
                _effect.emit(PrayerEffect.ShowError(it.message))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        prayCountJobs.values.forEach { it.cancel() }
        prayCountJobs.clear()
        prayersJob?.cancel()
    }
}
