package com.medbox.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A label such as "Ağrı Kesici", "Antibiyotik" or "Çocuk" used to categorize medicines.
 */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** ARGB color used for the tag chip, e.g. 0xFF1E88E5. */
    val color: Long
)

object DefaultTags {
    val presets = listOf(
        "Ağrı Kesici" to 0xFFE53935L,
        "Antibiyotik" to 0xFF8E24AAL,
        "Ateş Düşürücü" to 0xFFFB8C00L,
        "Vitamin / Takviye" to 0xFF43A047L,
        "Soğuk Algınlığı / Öksürük" to 0xFF00897BL,
        "Mide / Sindirim" to 0xFF6D4C41L,
        "Alerji" to 0xFFFDD835L,
        "Cilt / Merhem" to 0xFFF06292L,
        "Çocuk İçin" to 0xFF29B6F6L,
        "Kronik Kullanım" to 0xFF5E35B1L
    )
}
