package com.dailydevchallenge.devstreaks.database
import com.dailydevchallenge.database.Users


import com.dailydevchallenge.devstreaks.model.User
import kotlinx.datetime.Instant

// Assuming your .sq file uses a named query like `getUserById`
fun Users.toModel(): User {
    return User(
        userId = userId,
        email = email,
        passwordHash = passwordHash,
        username = username,
        avatarUrl = avatarUrl,
        createdAt = createdAt,
        lastLogin = lastLogin,
        xp = xp?.toInt() ?: 0,
        level = level,
        dailyStreak = dailyStreak,
        streakStartDate = streakStartDate?.let { Instant.fromEpochMilliseconds(it) },
        preferences = parsePreferences(preferences),
        role = role.toString(),
        badges = badges?.split("|") ?: emptyList()
    )
}

