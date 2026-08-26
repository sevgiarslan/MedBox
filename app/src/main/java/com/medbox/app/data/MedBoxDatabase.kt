package com.medbox.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Medicine::class, Tag::class, MedicineTagCrossRef::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedBoxDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var instance: MedBoxDatabase? = null

        fun getInstance(context: Context): MedBoxDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedBoxDatabase::class.java,
                    "medbox.db"
                ).build().also { instance = it }
            }
    }
}
