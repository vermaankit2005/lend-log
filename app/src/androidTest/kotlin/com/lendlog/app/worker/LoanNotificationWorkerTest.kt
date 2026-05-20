package com.lendlog.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.lendlog.app.data.datastore.AppPreferences
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoanNotificationWorkerTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var appPreferences: AppPreferences

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun worker_succeedsForDueSoonLoan() = runBlocking {
        val worker = buildWorker(isOverdue = false, reminderDays = 3)
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun worker_succeedsForOverdueLoan() = runBlocking {
        val worker = buildWorker(isOverdue = true, reminderDays = 3)
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun worker_failsWhenRequiredDataMissing() = runBlocking {
        val worker = TestListenableWorkerBuilder<LoanNotificationWorker>(context)
            .setWorkerFactory(workerFactory())
            .setInputData(workDataOf()) // no loan_id / item_name / borrower_name
            .build()
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun worker_succeedsWithCustomReminderDays() = runBlocking {
        val worker = buildWorker(isOverdue = false, reminderDays = 7)
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun worker_succeedsWithSingleReminderDay() = runBlocking {
        val worker = buildWorker(isOverdue = false, reminderDays = 1)
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    // ---- helpers ----

    private fun buildWorker(isOverdue: Boolean, reminderDays: Int) =
        TestListenableWorkerBuilder<LoanNotificationWorker>(context)
            .setWorkerFactory(workerFactory())
            .setInputData(
                workDataOf(
                    LoanNotificationWorker.KEY_LOAN_ID        to "test-loan-id",
                    LoanNotificationWorker.KEY_ITEM_NAME      to "Camera",
                    LoanNotificationWorker.KEY_BORROWER_NAME  to "Alice",
                    LoanNotificationWorker.KEY_IS_OVERDUE     to isOverdue,
                    LoanNotificationWorker.KEY_REMINDER_DAYS  to reminderDays
                )
            )
            .build()

    private fun workerFactory() = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker = LoanNotificationWorker(appContext, workerParameters, appPreferences)
    }
}
