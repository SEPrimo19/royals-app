package com.grace.app.presentation.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.BuildConfig
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.AppVersion
import com.grace.app.domain.usecase.appversion.CheckForUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateBannerUiState(
    val showBanner: Boolean = false,
    val showBlocker: Boolean = false,
    val update: AppVersion? = null
)

@HiltViewModel
class UpdateBannerViewModel @Inject constructor(
    private val checkForUpdate: CheckForUpdateUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateBannerUiState())
    val uiState: StateFlow<UpdateBannerUiState> = _uiState.asStateFlow()

    init {
        check()
    }

    fun recheck() = check()

    private fun check() {
        viewModelScope.launch {
            val update = checkForUpdate(BuildConfig.VERSION_CODE) ?: run {
                _uiState.update { UpdateBannerUiState() }
                return@launch
            }
            if (update.isMandatory) {
                _uiState.update {
                    UpdateBannerUiState(
                        showBanner = false,
                        showBlocker = true,
                        update = update
                    )
                }
                return@launch
            }
            val dismissed = prefs.dismissedUpdateForVersion.first()
            val shouldShow = dismissed < update.versionCode
            _uiState.update {
                UpdateBannerUiState(
                    showBanner = shouldShow,
                    showBlocker = false,
                    update = update
                )
            }
        }
    }

    fun dismiss() {
        val v = _uiState.value.update ?: return
        viewModelScope.launch {
            prefs.setDismissedUpdateForVersion(v.versionCode)
            _uiState.update { it.copy(showBanner = false) }
        }
    }
}
