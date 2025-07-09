package com.dailydevchallenge.devstreaks.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.dailydevchallenge.devstreaks.R



class DailyNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "DevStreak Reminder"
        val message = inputData.getString("message") ?: "Did you complete your daily dev challenge?"
        val type = inputData.getString("type") ?: "default"
        showNotification(title, message, type)
        return Result.success()
    }

    private fun showNotification(title: String, message: String, type: String) {
        val channelId = "daily_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Daily Reminder", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        // ✅ Intent to open MainActivity (or launcher activity)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("navigateTo", type)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // ✅ CORRECT
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // ✅ Set click action
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

}
//manager.notify(System.currentTimeMillis().toInt(), notification)