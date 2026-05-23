package com.lendlog.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
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
    }

    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
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
                val crashDir = File(filesDir, "crash_logs").also { it.mkdirs() }
                File(crashDir, "crash-$timestamp.txt").writeText(log)
            } catch (_: Exception) {}
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
            .setRequiresCharging(true)
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
