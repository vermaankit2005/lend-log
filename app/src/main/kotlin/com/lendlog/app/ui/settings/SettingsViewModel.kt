package com.lendlog.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lendlog.app.data.repository.LoanRepository
import com.lendlog.app.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class PrefsSnapshot(
    val unlocked: Boolean,
    val notif: Boolean,
    val days: Int,
    val ts: Long,
    val autoSms: Boolean
)

data class SettingsUiState(
    val isUnlocked: Boolean = false,
    val showPaywall: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val reminderDays: Int = 3,
    val lastBackupTimestamp: Long = 0L,
    val isExporting: Boolean = false,
    val exportResult: Boolean? = null,
    val isRestoring: Boolean = false,
    val restoreResult: Boolean? = null,
    val autoSmsEnabled: Boolean = false,
    val showAutoSmsConfirm: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.isUnlocked,
                repository.notificationsEnabled,
                repository.reminderDays,
                repository.lastBackupTimestamp,
                repository.autoSmsEnabled
            ) { unlocked, notif, days, ts, autoSms ->
                PrefsSnapshot(unlocked, notif, days, ts, autoSms)
            }.collect { snap ->
                _uiState.update { it.copy(
                    isUnlocked           = snap.unlocked,
                    notificationsEnabled = snap.notif,
                    reminderDays         = snap.days,
                    lastBackupTimestamp  = snap.ts,
                    autoSmsEnabled       = snap.autoSms
                ) }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
            val loans = repository.activeLoans.first()
            notificationScheduler.cancelAll(loans.map { it.id })
            if (enabled) {
                val days = repository.reminderDays.first()
                loans.forEach { notificationScheduler.scheduleForLoan(it, days) }
            }
        }
    }

    fun setReminderDays(days: Int) {
        viewModelScope.launch {
            repository.setReminderDays(days)
            val notifEnabled = repository.notificationsEnabled.first()
            if (notifEnabled) {
                val loans = repository.activeLoans.first()
                notificationScheduler.cancelAll(loans.map { it.id })
                loans.forEach { notificationScheduler.scheduleForLoan(it, days) }
            }
        }
    }

    fun showPaywall() = _uiState.update { it.copy(showPaywall = true) }
    fun dismissPaywall() = _uiState.update { it.copy(showPaywall = false) }
    fun onPurchased() {
        viewModelScope.launch {
            repository.setUnlocked(true)
            _uiState.update { it.copy(showPaywall = false) }
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

    fun onAutoSmsToggled(enabling: Boolean) {
        if (enabling) {
            _uiState.update { it.copy(showAutoSmsConfirm = true) }
        } else {
            viewModelScope.launch { repository.setAutoSmsEnabled(false) }
        }
    }

    fun confirmAutoSms() {
        _uiState.update { it.copy(showAutoSmsConfirm = false) }
    }

    fun dismissAutoSmsConfirm() {
        _uiState.update { it.copy(showAutoSmsConfirm = false) }
    }

    fun setAutoSmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSmsEnabled(enabled)
            if (enabled) {
                val loans = repository.activeLoans.first()
                val days = repository.reminderDays.first()
                loans.forEach { notificationScheduler.scheduleForLoan(it, days) }
            }
        }
    }
}
