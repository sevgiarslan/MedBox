package com.medbox.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.medbox.app.MainActivity
import com.medbox.app.R
import android.app.PendingIntent
import android.content.Intent

object NotificationHelper {
    const val CHANNEL_ID = "expiration_alerts"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showExpirySummary(context: Context, expiredCount: Int, expiringSoonCount: Int) {
        if (expiredCount == 0 && expiringSoonCount == 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = if (expiredCount > 0) {
            context.getString(R.string.notification_title_expired)
        } else {
            context.getString(R.string.notification_title_expiring)
        }

        val bodyParts = mutableListOf<String>()
        if (expiredCount > 0) bodyParts += "$expiredCount ilacın süresi dolmuş"
        if (expiringSoonCount > 0) bodyParts += "$expiringSoonCount ilacın süresi yakında doluyor"
        val body = bodyParts.joinToString(", ")

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
