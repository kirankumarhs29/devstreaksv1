package com.dailydevchallenge.devstreaks.notification

interface NotificationScheduler {
    fun scheduleDailyReminderNotification(hour: Int, minute: Int)
    fun cancelDailyReminderNotification()
    fun scheduleOneTimeNotification(title: String, message: String, type: String)
}

expect fun getNotificationScheduler(): NotificationScheduler

// commonMain
interface PushMessageHandler {
    fun onPushReceived(title: String, message: String, data: Map<String, String>)
}

expect fun getPushMessageHandler(): PushMessageHandler

