package com.medbox.app.util

import android.content.Context
import com.medbox.app.data.MedBoxDatabase
import com.medbox.app.data.MedicineRepository

/** Simple manual dependency container, avoids pulling in a DI framework for a small app. */
class AppContainer(context: Context) {
    val repository: MedicineRepository by lazy {
        MedicineRepository(MedBoxDatabase.getInstance(context))
    }
}
