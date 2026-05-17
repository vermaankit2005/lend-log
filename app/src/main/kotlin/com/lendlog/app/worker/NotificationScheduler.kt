package com.lendlog.app.worker

import android.content.Context
import androidx.work.*
import com.lendlog.app.data.db.Loan
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleForLoan(loan: Loan, reminderDays: Int = 3) {
        if (loan.isReturned) return

        val now = System.currentTimeMillis()
        val reminderBefore = loan.returnDate - TimeUnit.DAYS.toMillis(reminderDays.toLong())
        val overdueAt = loan.returnDate

        if (reminderBefore > now) {
            val delay = reminderBefore - now
            val request = buildNotifRequest(
                loanId = loan.id,
                itemName = loan.itemName,
                borrowerName = loan.borrowerName,
                isOverdue = false,
                delayMillis = delay
            )
            workManager.enqueueUniqueWork(
                "notif_due_soon_${loan.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        if (overdueAt > now) {
            val delay = overdueAt - now
            val request = buildNotifRequest(
                loanId = loan.id,
                itemName = loan.itemName,
                borrowerName = loan.borrowerName,
                isOverdue = true,
                delayMillis = delay
            )
            workManager.enqueueUniqueWork(
                "notif_overdue_${loan.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancelForLoan(loanId: String) {
        workManager.cancelUniqueWork("notif_due_soon_$loanId")
        workManager.cancelUniqueWork("notif_overdue_$loanId")
    }

    fun cancelAll(loanIds: List<String>) = loanIds.forEach { cancelForLoan(it) }

    private fun buildNotifRequest(
        loanId: String,
        itemName: String,
        borrowerName: String,
        isOverdue: Boolean,
        delayMillis: Long
    ): OneTimeWorkRequest {
        val data = workDataOf(
            LoanNotificationWorker.KEY_LOAN_ID to loanId,
            LoanNotificationWorker.KEY_ITEM_NAME to itemName,
            LoanNotificationWorker.KEY_BORROWER_NAME to borrowerName,
            LoanNotificationWorker.KEY_IS_OVERDUE to isOverdue
        )
        return OneTimeWorkRequestBuilder<LoanNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
    }
}
