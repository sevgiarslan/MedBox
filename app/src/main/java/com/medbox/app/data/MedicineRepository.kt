package com.medbox.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class MedicineRepository(private val db: MedBoxDatabase) {

    private val medicineDao = db.medicineDao()
    private val tagDao = db.tagDao()

    fun observeMedicines(): Flow<List<MedicineWithTags>> = medicineDao.observeAllWithTags()

    fun observeMedicine(id: Long): Flow<MedicineWithTags?> = medicineDao.observeByIdWithTags(id)

    fun observeTags(): Flow<List<Tag>> = tagDao.observeAll()

    suspend fun findByBarcode(barcode: String): MedicineWithTags? = medicineDao.findByBarcode(barcode)

    suspend fun getExpiringOrExpired(withinDays: Long, today: LocalDate = LocalDate.now()): List<MedicineWithTags> =
        medicineDao.getExpiringOrExpired(today.plusDays(withinDays))

    suspend fun saveMedicine(medicine: Medicine, tagIds: List<Long>): Long {
        val id = medicineDao.insert(medicine)
        medicineDao.setTagsForMedicine(id, tagIds)
        return id
    }

    suspend fun deleteMedicine(medicine: Medicine) = medicineDao.delete(medicine)

    suspend fun createTag(name: String, color: Long): Long = tagDao.insert(Tag(name = name, color = color))

    suspend fun ensureDefaultTags() {
        if (tagDao.count() == 0) {
            tagDao.insertAll(DefaultTags.presets.map { (name, color) -> Tag(name = name, color = color) })
        }
    }
}
