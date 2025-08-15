package com.dailydevchallenge.devstreaks.notification

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.*
import com.dailydevchallenge.devstreaks.network.getHttpClient
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.core.content.edit


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
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")

        // Store token locally for immediate access
        storeTokenLocally(token)

        // Send token to your backend server
        sendTokenToServer(token)
    }

    private fun storeTokenLocally(token: String) {
        try {
            val sharedPrefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit { putString("fcm_token", token) }
            Log.d("FCM", "Token stored locally successfully")
        } catch (e: Exception) {
            Log.e("FCM", "Failed to store token locally", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(DelicateCoroutinesApi::class)
    private fun sendTokenToServer(token: String) {
        try {
            // You can implement this using your existing HTTP client
            // For now, we'll use a coroutine to handle the async call
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    // Replace with your actual backend endpoint
                    sendTokenToBackend(token)
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to send token to backend", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error in token update process", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun sendTokenToBackend(token: String) {
        try {
            val userId = UserPreferences.getSafeUserId()

            val client = getHttpClient()

            client.post("https://your-backend.com/api/fcm/token") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(mapOf(
                    "userId" to userId,
                    "fcmToken" to token,
                    "platform" to "android"
                ))
            }

            Log.d("FCM", "Token successfully sent to backend")
        } catch (e: Exception) {
            Log.e("FCM", "Failed to send token to backend: ${e.message}", e)
            // You might want to store this for retry later
            storeFailedTokenUpdate(token)
        }
    }

    private fun storeFailedTokenUpdate(token: String) {
        // Store failed token updates for retry later
        val sharedPrefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit { putString("pending_token_update", token) }
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