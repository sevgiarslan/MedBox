package com.medbox.app.data

import androidx.room.Entity

@Entity(tableName = "medicine_tag_cross_ref", primaryKeys = ["medicineId", "tagId"])
data class MedicineTagCrossRef(
    val medicineId: Long,
    val tagId: Long
)
