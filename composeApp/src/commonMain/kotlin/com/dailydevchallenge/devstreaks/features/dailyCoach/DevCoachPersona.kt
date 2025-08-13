package com.dailydevchallenge.devstreaks.features.dailyCoach

object DevCoachPersona {
    fun introForSession(sessionTitle: String): String {
        return "Welcome to your daily coaching session on $sessionTitle! Let's make the most of it together."
    }
    fun praise(xp: Int): String{
        return "Great job! You've earned $xp XP for your efforts today."
    }
    fun failureFeedback(mistakeType: String): String {
        return "Don't worry about the $mistakeType. Every mistake is a learning opportunity. Let's try again!"
    }
    fun dailyTip(): String {
        return "Here's a tip for today: Consistency is key! Small, daily actions lead to big results over time. Keep going!"
    }
}
