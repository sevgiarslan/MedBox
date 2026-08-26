package com.medbox.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MedicineDao {

    @Transaction
    @Query("SELECT * FROM medicines ORDER BY expirationDate ASC")
    fun observeAllWithTags(): Flow<List<MedicineWithTags>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE id = :id")
    fun observeByIdWithTags(id: Long): Flow<MedicineWithTags?>

    @Transaction
    @Query("SELECT * FROM medicines WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): MedicineWithTags?

    @Transaction
    @Query("SELECT * FROM medicines WHERE expirationDate <= :cutoff ORDER BY expirationDate ASC")
    suspend fun getExpiringOrExpired(cutoff: LocalDate): List<MedicineWithTags>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: Medicine): Long

    @Delete
    suspend fun delete(medicine: Medicine)

    @Query("DELETE FROM medicine_tag_cross_ref WHERE medicineId = :medicineId")
    suspend fun clearTagsForMedicine(medicineId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<MedicineTagCrossRef>)

    @Transaction
    suspend fun setTagsForMedicine(medicineId: Long, tagIds: List<Long>) {
        clearTagsForMedicine(medicineId)
        insertCrossRefs(tagIds.map { MedicineTagCrossRef(medicineId, it) })
    }
}
