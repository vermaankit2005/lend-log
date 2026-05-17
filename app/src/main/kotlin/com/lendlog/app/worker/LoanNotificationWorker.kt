package com.lendlog.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lendlog.app.LendLogApp
import com.lendlog.app.MainActivity
import com.lendlog.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LoanNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_LOAN_ID = "loan_id"
        const val KEY_ITEM_NAME = "item_name"
        const val KEY_BORROWER_NAME = "borrower_name"
        const val KEY_IS_OVERDUE = "is_overdue"
    }

    override suspend fun doWork(): Result {
        val loanId = inputData.getString(KEY_LOAN_ID) ?: return Result.failure()
        val itemName = inputData.getString(KEY_ITEM_NAME) ?: return Result.failure()
        val borrowerName = inputData.getString(KEY_BORROWER_NAME) ?: return Result.failure()
        val isOverdue = inputData.getBoolean(KEY_IS_OVERDUE, false)

        val title: String
        val body: String

        if (isOverdue) {
            title = "$itemName is overdue"
            body = "$borrowerName still has your $itemName"
        } else {
            title = "$itemName due soon"
            body = "$borrowerName borrowed your $itemName — due in 3 days"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(loanId.hashCode(), notification)

        return Result.success()
    }
}
