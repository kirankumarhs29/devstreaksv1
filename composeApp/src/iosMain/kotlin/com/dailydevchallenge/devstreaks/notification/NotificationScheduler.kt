package com.dailydevchallenge.devstreaks.notification
import platform.Foundation.*
import platform.UserNotifications.*

actual fun getNotificationScheduler(): NotificationScheduler = IOSNotificationScheduler()

actual fun getPushMessageHandler(): PushMessageHandler = IOSPushMessageHandler()

class IOSNotificationScheduler : NotificationScheduler {

    override fun scheduleDailyReminderNotification(hour: Int, minute: Int) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Daily Reminder")
            setBody("Don't forget to check your dev streak!")
        }

        val triggerDate = NSDateComponents().apply {
            this.hour = hour.toLong()
            this.minute = minute.toLong()
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            triggerDate,
            repeats = true
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "daily_reminder",
            content = content,
            trigger = trigger
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
            request,
            null
        )
    }

    override fun scheduleOneTimeNotification(title: String, message: String, type: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Daily Reminder")
            setBody("Don't forget to check your dev streak!")
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "onetime_$type",
            content = content,
            trigger = trigger
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
            request,
            null
        )
    }

    override fun cancelDailyReminderNotification() {
        UNUserNotificationCenter.currentNotificationCenter().removePendingNotificationRequestsWithIdentifiers(
            listOf("daily_reminder")
        )
    }
}

class IOSPushMessageHandler : PushMessageHandler {
    override fun onPushReceived(title: String, message: String, data: Map<String, String>) {
        IOSNotificationScheduler().scheduleOneTimeNotification(title, message, data["type"] ?: "general")
    }
}
