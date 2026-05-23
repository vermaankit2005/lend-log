package com.lendlog.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lendlog.app.data.db.Loan
import com.lendlog.app.data.repository.LoanRepository
import com.lendlog.app.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject



data class DetailUiState(
    val loan: Loan? = null,
    val showDeleteDialog: Boolean = false,
    val showReturnDialog: Boolean = false,
    val deleted: Boolean = false,
    val showConfetti: Boolean = false,
    val autoSmsEnabled: Boolean = false,
    val smsNudgeTipShown: Boolean = false
)

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val notificationScheduler: NotificationScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val loanId: String = checkNotNull(savedStateHandle["loanId"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeLoan(loanId),
                repository.autoSmsEnabled,
                repository.smsNudgeTipShown
            ) { loan, autoSms, tipShown -> Triple(loan, autoSms, tipShown) }
            .collect { (loan, autoSms, tipShown) ->
                _uiState.update { it.copy(loan = loan, autoSmsEnabled = autoSms, smsNudgeTipShown = tipShown) }
            }
        }
    }

    fun showDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = true) }
    fun dismissDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = false) }
    fun showReturnDialog() = _uiState.update { it.copy(showReturnDialog = true) }
    fun dismissReturnDialog() = _uiState.update { it.copy(showReturnDialog = false) }

    fun deleteLoan() {
        viewModelScope.launch {
            notificationScheduler.cancelForLoan(loanId)
            repository.deleteLoan(loanId)
            _uiState.update { it.copy(deleted = true, showDeleteDialog = false) }
        }
    }

    fun markSmsNudgeTipShown() {
        viewModelScope.launch { repository.setSmsNudgeTipShown(true) }
    }

    fun markReturned() {
        viewModelScope.launch {
            notificationScheduler.cancelForLoan(loanId)
            repository.markReturned(loanId)
            _uiState.update { it.copy(showReturnDialog = false, showConfetti = true) }
        }
    }
}
