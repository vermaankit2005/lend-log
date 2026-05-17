package com.lendlog.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lendlog.app.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val lastBackupTimestamp: Long = 0L,
    val isExporting: Boolean = false,
    val exportResult: Boolean? = null,
    val isRestoring: Boolean = false,
    val restoreResult: Boolean? = null,
    val isUnlocked: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.lastBackupTimestamp,
                repository.isUnlocked
            ) { ts, unlocked -> ts to unlocked }
                .collect { (ts, unlocked) ->
                    _uiState.update { it.copy(lastBackupTimestamp = ts, isUnlocked = unlocked) }
                }
        }
    }

    fun exportNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportResult = null) }
            val success = repository.exportToDownloads()
            _uiState.update { it.copy(isExporting = false, exportResult = success) }
        }
    }

    fun restoreFromJson(jsonContent: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, restoreResult = null) }
            val success = repository.restoreFromJson(jsonContent)
            _uiState.update { it.copy(isRestoring = false, restoreResult = success) }
        }
    }

    fun clearExportResult() = _uiState.update { it.copy(exportResult = null) }
    fun clearRestoreResult() = _uiState.update { it.copy(restoreResult = null) }
}
