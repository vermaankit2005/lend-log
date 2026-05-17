package com.lendlog.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        installCrashLogger()
        super.onCreate()
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
                val fileName = "lendlog-crash-$timestamp.txt"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(log) }
                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val dir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    File(dir, fileName).writeText(log)
                }
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
            .setRequiresDeviceIdle(true)
            .build()

        val request = PeriodicWorkRequestBuilder<NightlyBackupWorker>(1, TimeUnit.DAYS)
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
