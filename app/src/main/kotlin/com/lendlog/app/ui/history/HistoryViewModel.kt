package com.lendlog.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lendlog.app.data.db.Loan
import com.lendlog.app.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HistoryUiState(val loans: List<Loan> = emptyList())

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: LoanRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository.returnedLoans
        .map { HistoryUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )
}
