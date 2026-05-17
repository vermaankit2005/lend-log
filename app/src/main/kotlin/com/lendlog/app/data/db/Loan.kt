package com.lendlog.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String,
    val itemName: String,
    val notes: String? = null,
    val photoUri: String? = null,
    val borrowerName: String,
    val borrowerContactId: String? = null,
    val borrowerPhone: String? = null,
    val returnDate: Long,
    val lentDate: Long,
    val isReturned: Boolean = false,
    val returnedDate: Long? = null,
    val tags: String = "",
    val createdAt: Long
) {
    val isOverdue: Boolean
        get() = !isReturned && returnDate < System.currentTimeMillis()

    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
