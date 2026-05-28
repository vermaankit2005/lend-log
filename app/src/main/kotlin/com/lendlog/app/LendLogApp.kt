package com.lendlog.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.lendlog.app.billing.BillingManager
import com.lendlog.app.worker.NightlyBackupWorker
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class LendLogApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var billingManager: BillingManager

    private val _workManagerConfiguration by lazy {
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()
    }

    override val workManagerConfiguration: Configuration
        get() = _workManagerConfiguration

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        createNotificationChannels()
        scheduleNightlyBackup()
        billingManager.connect()
    }

    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Capture the log text on the crashing thread (stack trace must be read here),
            // then write to disk on a separate thread to avoid deadlocking on held locks.
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val log = buildString {
                appendLine("=== LendLog Crash Log $timestamp ===")
                appendLine("Thread: ${thread.name}")
                appendLine()
                appendLine(throwable.stackTraceToString())
                var cause = throwable.cause
                while (cause != null) {
                    appendLine("Caused by:")
                    appendLine(cause.stackTraceToString())
                    cause = cause.cause
                }
            }
            Thread {
                try {
                    val crashDir = File(filesDir, "crash_logs").also { it.mkdirs() }
                    File(crashDir, "crash-$timestamp.txt").writeText(log)
                } catch (_: Exception) {}
            }.start()
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notif_channel_reminders_desc)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun scheduleNightlyBackup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) setRequiresDeviceIdle(true)
            }
            .build()

        val request = PeriodicWorkRequestBuilder<NightlyBackupWorker>(1, TimeUnit.DAYS, 4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NightlyBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "loan_reminders"
    }
}
