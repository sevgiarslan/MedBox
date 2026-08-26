package com.medbox.app.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class MedicineWithTags(
    @Embedded
    val medicine: Medicine,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MedicineTagCrossRef::class,
            parentColumn = "medicineId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)
