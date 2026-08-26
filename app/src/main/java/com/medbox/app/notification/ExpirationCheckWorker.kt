package com.medbox.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.medbox.app.MedBoxApplication
import com.medbox.app.data.EXPIRING_SOON_THRESHOLD_DAYS
import com.medbox.app.data.ExpiryStatus
import com.medbox.app.data.expiryStatus
import java.util.concurrent.TimeUnit

class ExpirationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as MedBoxApplication).container.repository
        val candidates = repository.getExpiringOrExpired(EXPIRING_SOON_THRESHOLD_DAYS)

        var expired = 0
        var expiringSoon = 0
        candidates.forEach {
            when (it.medicine.expiryStatus()) {
                ExpiryStatus.EXPIRED -> expired++
                ExpiryStatus.EXPIRING_SOON -> expiringSoon++
                ExpiryStatus.OK -> Unit
            }
        }

        NotificationHelper.showExpirySummary(applicationContext, expired, expiringSoon)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "expiration_check_daily"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
