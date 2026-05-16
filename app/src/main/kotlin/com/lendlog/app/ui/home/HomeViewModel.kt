package com.lendlog.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lendlog.app.data.db.Loan
import com.lendlog.app.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class FeedView { BY_ITEM, BY_PERSON }
enum class FilterType { ALL, OVERDUE }

data class HomeUiState(
    val loans: List<Loan> = emptyList(),
    val feedView: FeedView = FeedView.BY_ITEM,
    val filter: FilterType = FilterType.ALL,
    val activeLoanCount: Int = 0,
    val isUnlocked: Boolean = false
) {
    val displayedLoans: List<Loan>
        get() {
            val filtered = when (filter) {
                FilterType.ALL -> loans
                FilterType.OVERDUE -> loans.filter { it.isOverdue }
            }
            return filtered.sortedWith(compareByDescending<Loan> { it.isOverdue }.thenBy { it.returnDate })
        }

    val groupedByPerson: Map<String, List<Loan>>
        get() = displayedLoans.groupBy { it.borrowerName }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _feedView = MutableStateFlow(FeedView.BY_ITEM)
    private val _filter = MutableStateFlow(FilterType.ALL)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.activeLoans,
        _feedView,
        _filter,
        repository.activeLoanCount,
        repository.isUnlocked
    ) { loans, feedView, filter, count, unlocked ->
        HomeUiState(
            loans = loans,
            feedView = feedView,
            filter = filter,
            activeLoanCount = count,
            isUnlocked = unlocked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun setFeedView(view: FeedView) { _feedView.value = view }
    fun setFilter(filter: FilterType) { _filter.value = filter }
}
