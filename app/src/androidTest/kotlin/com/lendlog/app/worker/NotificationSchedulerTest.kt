package com.lendlog.app.worker

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.lendlog.app.data.db.Loan
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NotificationSchedulerTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var scheduler: NotificationScheduler

    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun scheduleForLoan_enqueuesBothWorkRequests() {
        val loan = futureLoan(daysFromNow = 10)
        scheduler.scheduleForLoan(loan, reminderDays = 3)

        val dueSoonInfo = workManager
            .getWorkInfosForUniqueWork("notif_due_soon_${loan.id}")
            .get()
        val overdueInfo = workManager
            .getWorkInfosForUniqueWork("notif_overdue_${loan.id}")
            .get()

        assertEquals(1, dueSoonInfo.size)
        assertEquals(WorkInfo.State.ENQUEUED, dueSoonInfo.first().state)
        assertEquals(1, overdueInfo.size)
        assertEquals(WorkInfo.State.ENQUEUED, overdueInfo.first().state)
    }

    @Test
    fun scheduleForLoan_skipsReturnedLoan() {
        val loan = futureLoan(daysFromNow = 10).copy(isReturned = true)
        scheduler.scheduleForLoan(loan)

        val dueSoonInfo = workManager
            .getWorkInfosForUniqueWork("notif_due_soon_${loan.id}")
            .get()

        assertTrue(dueSoonInfo.isEmpty())
    }

    @Test
    fun scheduleForLoan_skipsDueSoonWhenReturnDateIsLessThanReminderDaysAway() {
        // return date is 1 day from now but reminder = 3 days → due-soon window already passed
        val loan = futureLoan(daysFromNow = 1)
        scheduler.scheduleForLoan(loan, reminderDays = 3)

        val dueSoonInfo = workManager
            .getWorkInfosForUniqueWork("notif_due_soon_${loan.id}")
            .get()

        assertTrue("Due-soon should not be scheduled", dueSoonInfo.isEmpty())
    }

    @Test
    fun cancelForLoan_removesEnqueuedWork() {
        val loan = futureLoan(daysFromNow = 10)
        scheduler.scheduleForLoan(loan, reminderDays = 3)
        scheduler.cancelForLoan(loan.id)

        val dueSoonInfo = workManager
            .getWorkInfosForUniqueWork("notif_due_soon_${loan.id}")
            .get()
        val overdueInfo = workManager
            .getWorkInfosForUniqueWork("notif_overdue_${loan.id}")
            .get()

        val dueSoonCancelled = dueSoonInfo.isEmpty() ||
                dueSoonInfo.first().state == WorkInfo.State.CANCELLED
        val overdueCancelled = overdueInfo.isEmpty() ||
                overdueInfo.first().state == WorkInfo.State.CANCELLED

        assertTrue(dueSoonCancelled)
        assertTrue(overdueCancelled)
    }

    // ---- helpers ----

    private fun futureLoan(daysFromNow: Long) = Loan(
        id = "loan-${System.nanoTime()}",
        itemName = "Camera",
        notes = null,
        photoUri = null,
        borrowerName = "Alice",
        borrowerContactId = null,
        borrowerPhone = null,
        returnDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysFromNow),
        lentDate = System.currentTimeMillis(),
        isReturned = false,
        returnedDate = null,
        tags = "",
        createdAt = System.currentTimeMillis()
    )
}
