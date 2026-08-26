package com.medbox.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Barcode value (EAN-13, EAN-8, Code-128, ...) scanned or entered manually. Nullable. */
    val barcode: String? = null,
    val expirationDate: LocalDate,
    val quantity: Int = 1,
    val notes: String = "",
    val dateAdded: LocalDate = LocalDate.now()
)

enum class ExpiryStatus {
    EXPIRED,
    EXPIRING_SOON,
    OK
}

/** A medicine is considered "expiring soon" within this many days of today. */
const val EXPIRING_SOON_THRESHOLD_DAYS = 30L

fun Medicine.expiryStatus(today: LocalDate = LocalDate.now()): ExpiryStatus = when {
    expirationDate.isBefore(today) -> ExpiryStatus.EXPIRED
    !expirationDate.isAfter(today.plusDays(EXPIRING_SOON_THRESHOLD_DAYS)) -> ExpiryStatus.EXPIRING_SOON
    else -> ExpiryStatus.OK
}
