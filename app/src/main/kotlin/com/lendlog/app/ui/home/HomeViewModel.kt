package com.lendlog.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lendlog.app.data.db.Loan
import com.lendlog.app.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class FeedView { BY_STATUS, BY_PERSON }
enum class FilterType { ALL, OVERDUE }

data class HomeUiState(
    val loans: List<Loan> = emptyList(),
    val feedView: FeedView = FeedView.BY_STATUS,
    val filter: FilterType = FilterType.ALL,
    val activeLoanCount: Int = 0,
    val isUnlocked: Boolean = false
) {
    private val now get() = System.currentTimeMillis()
    private val threeDaysMs = 3L * 24 * 60 * 60 * 1000

    val overdueLoans: List<Loan>
        get() = loans.filter { it.isOverdue }
            .sortedByDescending { now - it.returnDate }   // most overdue first

    val dueSoonLoans: List<Loan>
        get() = loans.filter { !it.isOverdue && (it.returnDate - now) < threeDaysMs }
            .sortedBy { it.returnDate }                   // soonest first

    val activeLoans: List<Loan>
        get() = loans.filter { !it.isOverdue && (it.returnDate - now) >= threeDaysMs }
            .sortedByDescending { it.lentDate }           // newest first

    val displayedLoans: List<Loan>
        get() {
            val filtered = when (filter) {
                FilterType.ALL -> loans
                FilterType.OVERDUE -> loans.filter { it.isOverdue }
            }
            return filtered.sortedWith(compareByDescending<Loan> { it.isOverdue }.thenBy { it.returnDate })
        }

    val groupedByPerson: Map<String, List<Loan>>
        get() = displayedLoans
            .groupBy { it.borrowerName.trim().lowercase() }
            .entries.associate { (key, loans) ->
                // Title-case from the normalised key so casing variants ("john", "John") merge
                // into one deterministic display name regardless of insertion order.
                key.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } } to loans
            }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _feedView = MutableStateFlow(FeedView.BY_STATUS)
    private val _filter = MutableStateFlow(FilterType.ALL)

    // Ticks every minute so isOverdue transitions are reflected without a DB write.
    private val _ticker = flow {
        while (true) { emit(Unit); delay(60_000L) }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(repository.activeLoans, _feedView, _filter) { loans, view, filter ->
            Triple(loans, view, filter)
        },
        combine(repository.activeLoanCount, repository.isUnlocked) { count, unlocked ->
            count to unlocked
        },
        _ticker
    ) { (loans, feedView, filter), (count, unlocked), _ ->
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
