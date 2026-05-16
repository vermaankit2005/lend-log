package com.lendlog.app.ui.addloan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lendlog.app.data.repository.LoanRepository
import com.lendlog.app.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddLoanUiState(
    val itemName: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val borrowerName: String = "",
    val borrowerContactId: String? = null,
    val borrowerPhone: String? = null,
    val returnDate: Long? = null,
    val tags: String = "",
    val isSaving: Boolean = false,
    val savedLoanId: String? = null,
    val showPaywall: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean get() = itemName.isNotBlank() && borrowerName.isNotBlank() && returnDate != null
}

@HiltViewModel
class AddLoanViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddLoanUiState())
    val uiState: StateFlow<AddLoanUiState> = _uiState.asStateFlow()

    fun updateItemName(value: String) = _uiState.update { it.copy(itemName = value) }
    fun updateNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun updatePhotoUri(uri: String?) = _uiState.update { it.copy(photoUri = uri) }
    fun updateBorrower(name: String, contactId: String? = null, phone: String? = null) =
        _uiState.update { it.copy(borrowerName = name, borrowerContactId = contactId, borrowerPhone = phone) }
    fun updateReturnDate(epochMillis: Long) = _uiState.update { it.copy(returnDate = epochMillis) }
    fun updateTags(value: String) = _uiState.update { it.copy(tags = value) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun dismissPaywall() = _uiState.update { it.copy(showPaywall = false) }

    fun saveLoan() {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            if (!repository.canAddLoan()) {
                _uiState.update { it.copy(showPaywall = true) }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }
            val loan = repository.createNewLoan(
                itemName = state.itemName.trim(),
                notes = state.notes.trim().ifEmpty { null },
                photoUri = state.photoUri,
                borrowerName = state.borrowerName.trim(),
                borrowerContactId = state.borrowerContactId,
                borrowerPhone = state.borrowerPhone,
                returnDate = state.returnDate!!,
                tags = state.tags.trim()
            )
            repository.addLoan(loan)
            notificationScheduler.scheduleForLoan(loan)
            _uiState.update { it.copy(isSaving = false, savedLoanId = loan.id) }
        }
    }
}
