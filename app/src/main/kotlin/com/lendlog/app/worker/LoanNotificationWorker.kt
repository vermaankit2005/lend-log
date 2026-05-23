package com.lendlog.app.worker

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lendlog.app.LendLogApp
import com.lendlog.app.MainActivity
import com.lendlog.app.R
import com.lendlog.app.data.datastore.AppPreferences
import com.lendlog.app.data.db.LoanDao
import com.lendlog.app.util.SmsHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class LoanNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appPreferences: AppPreferences,
    private val loanDao: LoanDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_LOAN_ID        = "loan_id"
        const val KEY_ITEM_NAME      = "item_name"
        const val KEY_BORROWER_NAME  = "borrower_name"
        const val KEY_IS_OVERDUE     = "is_overdue"
        const val KEY_BORROWER_PHONE = "borrower_phone"
        const val KEY_REMINDER_DAYS  = "reminder_days"
    }

    override suspend fun doWork(): Result {
        val loanId        = inputData.getString(KEY_LOAN_ID)       ?: return Result.failure()
        val itemName      = inputData.getString(KEY_ITEM_NAME)     ?: return Result.failure()
        val borrowerName  = inputData.getString(KEY_BORROWER_NAME) ?: return Result.failure()
        val isOverdue     = inputData.getBoolean(KEY_IS_OVERDUE, false)
        val borrowerPhone = inputData.getString(KEY_BORROWER_PHONE)
        val reminderDays  = inputData.getInt(KEY_REMINDER_DAYS, 3)

        // Skip if the loan was returned after this work was enqueued
        val loan = loanDao.getLoanById(loanId)
        if (loan == null || loan.isReturned) return Result.success()

        postNotification(loanId, itemName, borrowerName, isOverdue, reminderDays)

        if (!borrowerPhone.isNullOrBlank() && appPreferences.autoSmsEnabled.first()) {
            SmsHelper.sendAutoSms(context, borrowerPhone, itemName)
        }

        return Result.success()
    }

    private fun postNotification(loanId: String, itemName: String, borrowerName: String, isOverdue: Boolean, reminderDays: Int = 3) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val title = if (isOverdue) "$itemName is overdue" else "$itemName due soon"
        val body  = if (isOverdue) "$borrowerName still has your $itemName"
                    else "$borrowerName borrowed your $itemName — due in $reminderDays ${if (reminderDays == 1) "day" else "days"}"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_LOAN_ID, loanId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, loanId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, LendLogApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(loanId.hashCode(), notification)
    }
}
