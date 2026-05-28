package com.lendlog.app.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.lendlog.app.data.datastore.AppPreferences
import com.lendlog.app.data.db.Loan
import com.lendlog.app.data.db.LoanDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) {
    val activeLoans: Flow<List<Loan>> = loanDao.observeActiveLoans()
    val returnedLoans: Flow<List<Loan>> = loanDao.observeReturnedLoans()
    val activeLoanCount: Flow<Int> = loanDao.observeActiveLoanCount()
    val isUnlocked: Flow<Boolean> = appPreferences.isUnlocked
    val lastBackupTimestamp: Flow<Long> = appPreferences.lastBackupTimestamp
    val themeMode: Flow<String> = appPreferences.themeMode
    val notificationsEnabled: Flow<Boolean> = appPreferences.notificationsEnabled
    val reminderDays: Flow<Int> = appPreferences.reminderDays
    // AUTO_SMS_DISABLED: flows preserved for future re-enable
    // val autoSmsEnabled: Flow<Boolean> = appPreferences.autoSmsEnabled
    // val smsNudgeTipShown: Flow<Boolean> = appPreferences.smsNudgeTipShown

    fun observeLoan(id: String): Flow<Loan?> = loanDao.observeLoanById(id)

    suspend fun addLoan(loan: Loan) = loanDao.insertLoan(loan)

    suspend fun updateLoan(loan: Loan) = loanDao.updateLoan(loan)

    suspend fun markReturned(loanId: String) {
        val loan = loanDao.getLoanById(loanId) ?: return
        loanDao.updateLoan(
            loan.copy(
                isReturned = true,
                returnedDate = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteLoan(loanId: String) {
        val loan = loanDao.getLoanById(loanId)
        loan?.photoUri?.let { deleteAppPhoto(it) }
        loanDao.deleteLoanById(loanId)
    }

    private fun deleteAppPhoto(uriString: String) {
        try {
            val lastSegment = android.net.Uri.parse(uriString).lastPathSegment ?: return
            File(context.filesDir, "loan_photos/${File(lastSegment).name}").delete()
        } catch (_: Exception) {}
    }

    suspend fun canAddLoan(): Boolean {
        val unlocked = appPreferences.isUnlocked.first()
        if (unlocked) return true
        return loanDao.getActiveLoanCount() < 3
    }

    suspend fun setUnlocked(unlocked: Boolean) = appPreferences.setUnlocked(unlocked)
    suspend fun setThemeMode(mode: String) = appPreferences.setThemeMode(mode)
    suspend fun setNotificationsEnabled(enabled: Boolean) = appPreferences.setNotificationsEnabled(enabled)
    suspend fun setReminderDays(days: Int) = appPreferences.setReminderDays(days)
    // AUTO_SMS_DISABLED: setters preserved for future re-enable
    // suspend fun setAutoSmsEnabled(enabled: Boolean) = appPreferences.setAutoSmsEnabled(enabled)
    // suspend fun setSmsNudgeTipShown(shown: Boolean) = appPreferences.setSmsNudgeTipShown(shown)

    fun createNewLoan(
        itemName: String,
        notes: String?,
        photoUri: String?,
        borrowerName: String,
        borrowerContactId: String?,
        borrowerPhone: String?,
        lentDate: Long,
        returnDate: Long,
        tags: String
    ) = Loan(
        id = UUID.randomUUID().toString(),
        itemName = itemName,
        notes = notes,
        photoUri = photoUri,
        borrowerName = borrowerName,
        borrowerContactId = borrowerContactId,
        borrowerPhone = borrowerPhone,
        returnDate = returnDate,
        lentDate = lentDate,
        isReturned = false,
        returnedDate = null,
        tags = tags,
        createdAt = System.currentTimeMillis()
    )

    suspend fun exportToDownloads(): Boolean = withContext(Dispatchers.IO) {
        try {
            val loans = loanDao.getAllLoansSnapshot()
            val json = Json { prettyPrint = true }.encodeToString(loans)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver

                // Delete any existing backup file to avoid duplicates
                resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                    arrayOf("lendlog-backup.json"),
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        resolver.delete(
                            ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id),
                            null, null
                        )
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "lendlog-backup.json")
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                val stream = resolver.openOutputStream(uri) ?: run {
                    resolver.delete(uri, null, null)
                    return@withContext false
                }
                stream.bufferedWriter().use { it.write(json) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(dir, "lendlog-backup.json").writeText(json)
            }
            appPreferences.setLastBackupTimestamp(System.currentTimeMillis())
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreFromJson(jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val lenientJson = Json { ignoreUnknownKeys = true }
            val loans = lenientJson.decodeFromString<List<Loan>>(jsonContent)
            if (loans.isEmpty()) return@withContext false
            // Delete photo files for loans being replaced before wiping the DB.
            loanDao.getAllLoansSnapshot().forEach { it.photoUri?.let { uri -> deleteAppPhoto(uri) } }
            loanDao.replaceAll(loans)
            true
        } catch (e: Exception) {
            false
        }
    }
}
