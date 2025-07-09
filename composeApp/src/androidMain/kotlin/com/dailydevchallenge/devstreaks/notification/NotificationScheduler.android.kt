package com.dailydevchallenge.devstreaks.notification

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.core.context.GlobalContext
import java.util.Calendar
import java.util.concurrent.TimeUnit


actual fun getNotificationScheduler(): NotificationScheduler {
    val context = GlobalContext.get().get<Context>()
    return AndroidNotificationScheduler(context)
}

actual fun getPushMessageHandler(): PushMessageHandler = AndroidPushMessageHandler()

class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {

    override fun scheduleDailyReminderNotification(hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, hour)
        target.set(Calendar.MINUTE, minute)
        target.set(Calendar.SECOND, 0)
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1) // Schedule for next day if time already passed today
        }
        val delay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyReminderNotification",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    override fun scheduleOneTimeNotification(title: String, message: String, type: String) {
        val workRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
            .setInputData(
                workDataOf(
                    "title" to title,
                    "message" to message,
                    "type" to type
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "OneTimeNotification-$type",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    override fun cancelDailyReminderNotification() {
        WorkManager.getInstance(context).cancelUniqueWork("DailyReminderNotification")
    }
}

// androidMain


class AndroidPushMessageHandler : PushMessageHandler {
    override fun onPushReceived(title: String, message: String, data: Map<String, String>) {
        Log.d("PushMessageHandler", "Received push: $title - $message with data: $data")

        // You can route this to NotificationScheduler
        val scheduler = getNotificationScheduler()
        val type = data["type"] ?: "general"
        scheduler.scheduleOneTimeNotification(title, message, type)
    }
}

class MyFirebaseService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 🔐 You MUST send this token to your backend or save it locally
        Log.d("FCM", "Refreshed token: $token")

        // Optionally: Store in SharedPreferences or your local database
        // Or sync to your server for targeted messaging
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "New Notification"
        val body = remoteMessage.notification?.body ?: ""
        val data = remoteMessage.data
        getPushMessageHandler().onPushReceived(title, body, data)
//        val scheduler = getNotificationScheduler()
//        scheduler.scheduleOneTimeNotification(title, body, data["type"] ?: "general")
    }
}