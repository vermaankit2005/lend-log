package com.lendlog.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Loan::class],
    version = 1,
    exportSchema = false
)
abstract class LoanDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao

    companion object {
        const val DATABASE_NAME = "loans.db"
    }
}
