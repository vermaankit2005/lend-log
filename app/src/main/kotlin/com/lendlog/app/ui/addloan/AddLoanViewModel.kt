package com.lendlog.app.ui.addloan

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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
    val lentDate: Long = System.currentTimeMillis(),
    val returnDate: Long? = null,
    val tags: String = "",
    val isSaving: Boolean = false,
    val savedLoanId: String? = null,
    val showPaywall: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
) {
    val isValid: Boolean get() = itemName.isNotBlank() && borrowerName.isNotBlank() && returnDate != null
}

@HiltViewModel
class AddLoanViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val notificationScheduler: NotificationScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editLoanId: String? = savedStateHandle.get<String>("loanId")

    private val _uiState = MutableStateFlow(AddLoanUiState(isEditMode = editLoanId != null))
    val uiState: StateFlow<AddLoanUiState> = _uiState.asStateFlow()

    init {
        editLoanId?.let { id ->
            viewModelScope.launch {
                val loan = repository.observeLoan(id).first()
                if (loan != null) {
                    _uiState.update {
                        it.copy(
                            itemName = loan.itemName,
                            notes = loan.notes ?: "",
                            photoUri = loan.photoUri,
                            borrowerName = loan.borrowerName,
                            borrowerContactId = loan.borrowerContactId,
                            borrowerPhone = loan.borrowerPhone,
                            lentDate = loan.lentDate,
                            returnDate = loan.returnDate,
                            tags = loan.tags
                        )
                    }
                }
            }
        }
    }

    fun updateItemName(value: String) = _uiState.update { it.copy(itemName = value) }
    fun updateNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun updatePhotoUri(uri: String?) = _uiState.update { it.copy(photoUri = uri) }
    fun updateBorrower(name: String, contactId: String? = null, phone: String? = null) =
        _uiState.update { it.copy(borrowerName = name, borrowerContactId = contactId, borrowerPhone = phone) }
    fun updateLentDate(epochMillis: Long) = _uiState.update { it.copy(lentDate = epochMillis) }
    fun updateReturnDate(epochMillis: Long) = _uiState.update { it.copy(returnDate = epochMillis) }
    fun updateTags(value: String) = _uiState.update { it.copy(tags = value) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun dismissPaywall() = _uiState.update { it.copy(showPaywall = false) }

    fun saveLoan() {
        val state = _uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            if (editLoanId == null && !repository.canAddLoan()) {
                _uiState.update { it.copy(showPaywall = true) }
                return@launch
            }
            doSaveLoan(state)
        }
    }

    fun onPurchased() {
        val state = _uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            repository.setUnlocked(true)
            doSaveLoan(state)
        }
    }

    private suspend fun doSaveLoan(state: AddLoanUiState) {
        _uiState.update { it.copy(isSaving = true) }
        if (editLoanId != null) {
            val existing = repository.observeLoan(editLoanId).first() ?: return
            val updated = existing.copy(
                itemName = state.itemName.trim().titleCase(),
                notes = state.notes.trim().ifEmpty { null },
                photoUri = state.photoUri,
                borrowerName = state.borrowerName.trim().titleCase(),
                borrowerContactId = state.borrowerContactId,
                borrowerPhone = state.borrowerPhone,
                lentDate = state.lentDate,
                returnDate = state.returnDate!!,
                tags = state.tags.trim()
            )
            repository.updateLoan(updated)
            notificationScheduler.cancelForLoan(editLoanId)
            val notifEnabled = repository.notificationsEnabled.first()
            val days = repository.reminderDays.first()
            if (notifEnabled && !updated.isReturned) notificationScheduler.scheduleForLoan(updated, days)
            _uiState.update { it.copy(isSaving = false, savedLoanId = editLoanId) }
        } else {
            val loan = repository.createNewLoan(
                itemName = state.itemName.trim().titleCase(),
                notes = state.notes.trim().ifEmpty { null },
                photoUri = state.photoUri,
                borrowerName = state.borrowerName.trim().titleCase(),
                borrowerContactId = state.borrowerContactId,
                borrowerPhone = state.borrowerPhone,
                lentDate = state.lentDate,
                returnDate = state.returnDate!!,
                tags = state.tags.trim()
            )
            repository.addLoan(loan)
            val notifEnabled = repository.notificationsEnabled.first()
            val days = repository.reminderDays.first()
            if (notifEnabled) notificationScheduler.scheduleForLoan(loan, days)
            _uiState.update { it.copy(isSaving = false, savedLoanId = loan.id) }
        }
    }
}

private fun String.titleCase(): String =
    split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
