package com.medbox.app.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
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

    /** Serializes the whole inventory to JSON so it can be shared with, or backed up by, someone else. */
    suspend fun exportAllAsJson(): String {
        val medicinesJson = JSONArray()
        medicineDao.getAllWithTagsOnce().forEach { withTags ->
            val medicineJson = JSONObject()
                .put("name", withTags.medicine.name)
                .put("barcode", withTags.medicine.barcode ?: JSONObject.NULL)
                .put("expirationDate", withTags.medicine.expirationDate.toString())
                .put("quantity", withTags.medicine.quantity)
                .put("notes", withTags.medicine.notes)
            val tagsJson = JSONArray()
            withTags.tags.forEach { tag ->
                tagsJson.put(JSONObject().put("name", tag.name).put("color", tag.color))
            }
            medicineJson.put("tags", tagsJson)
            medicinesJson.put(medicineJson)
        }
        return JSONObject()
            .put("version", 1)
            .put("medicines", medicinesJson)
            .toString(2)
    }

    /** Parses a JSON export (from this app, on this or another phone) and adds its medicines. Returns how many were imported. */
    suspend fun importFromJson(json: String): Int {
        val medicinesJson = JSONObject(json).optJSONArray("medicines") ?: JSONArray()
        var imported = 0
        for (i in 0 until medicinesJson.length()) {
            val medicineJson = medicinesJson.getJSONObject(i)
            val name = medicineJson.optString("name")
            if (name.isBlank()) continue
            val expirationDate = runCatching {
                LocalDate.parse(medicineJson.optString("expirationDate"))
            }.getOrNull() ?: continue
            val barcode = if (medicineJson.isNull("barcode")) null else medicineJson.optString("barcode").ifBlank { null }

            val tagIds = mutableListOf<Long>()
            val tagsJson = medicineJson.optJSONArray("tags")
            if (tagsJson != null) {
                for (j in 0 until tagsJson.length()) {
                    val tagJson = tagsJson.getJSONObject(j)
                    val tagName = tagJson.optString("name")
                    if (tagName.isBlank()) continue
                    tagIds += findOrCreateTagByName(tagName, tagJson.optLong("color", 0xFF9E9E9EL))
                }
            }

            val medicine = Medicine(
                name = name,
                barcode = barcode,
                expirationDate = expirationDate,
                quantity = medicineJson.optInt("quantity", 1),
                notes = medicineJson.optString("notes")
            )
            saveMedicine(medicine, tagIds)
            imported++
        }
        return imported
    }

    private suspend fun findOrCreateTagByName(name: String, color: Long): Long =
        tagDao.findByName(name)?.id ?: tagDao.insert(Tag(name = name, color = color))
}
