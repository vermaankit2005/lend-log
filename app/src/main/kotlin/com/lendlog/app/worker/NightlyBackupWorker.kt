package com.lendlog.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lendlog.app.data.repository.LoanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NightlyBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: LoanRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "nightly_backup"
    }

    override suspend fun doWork(): Result {
        return if (repository.exportToDownloads()) Result.success() else Result.retry()
    }
}
