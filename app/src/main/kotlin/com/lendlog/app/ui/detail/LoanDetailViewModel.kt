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
    val showConfetti: Boolean = false
    // AUTO_SMS_DISABLED: val autoSmsEnabled: Boolean = false,
    // AUTO_SMS_DISABLED: val smsNudgeTipShown: Boolean = false
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
        // AUTO_SMS_DISABLED: was a 3-flow combine with autoSmsEnabled + smsNudgeTipShown
        viewModelScope.launch {
            repository.observeLoan(loanId).collect { loan ->
                _uiState.update { it.copy(loan = loan) }
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

    // AUTO_SMS_DISABLED: fun markSmsNudgeTipShown() — re-enable after Play Store approves SEND_SMS
    /*
    fun markSmsNudgeTipShown() {
        viewModelScope.launch { repository.setSmsNudgeTipShown(true) }
    }
    */

    fun markReturned() {
        viewModelScope.launch {
            notificationScheduler.cancelForLoan(loanId)
            repository.markReturned(loanId)
            _uiState.update { it.copy(showReturnDialog = false, showConfetti = true) }
        }
    }

    fun clearConfetti() = _uiState.update { it.copy(showConfetti = false) }
}
