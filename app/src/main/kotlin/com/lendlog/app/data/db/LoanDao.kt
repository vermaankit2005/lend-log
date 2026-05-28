package com.lendlog.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans WHERE isReturned = 0 ORDER BY returnDate ASC")
    fun observeActiveLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE isReturned = 1 ORDER BY returnedDate DESC")
    fun observeReturnedLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans ORDER BY createdAt DESC")
    fun observeAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    fun observeLoanById(id: String): Flow<Loan?>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: String): Loan?

    @Query("SELECT COUNT(*) FROM loans WHERE isReturned = 0")
    fun observeActiveLoanCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loans WHERE isReturned = 0")
    suspend fun getActiveLoanCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan)

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: String)

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansSnapshot(): List<Loan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<Loan>)

    @Query("DELETE FROM loans")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(loans: List<Loan>) {
        deleteAll()
        insertAll(loans)
    }

    // Atomically checks the active count and inserts only if within the free-tier limit.
    // Returns true if inserted, false if the cap was already reached.
    @Transaction
    suspend fun insertIfWithinLimit(loan: Loan, limit: Int): Boolean {
        if (getActiveLoanCount() >= limit) return false
        insertLoan(loan)
        return true
    }
}
