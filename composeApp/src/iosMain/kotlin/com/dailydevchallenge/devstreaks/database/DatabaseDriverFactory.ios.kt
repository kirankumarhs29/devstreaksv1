package com.dailydevchallenge.devstreaks.database
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.dailydevchallenge.devstreaks.database.ChallengeDatabase


actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(ChallengeDatabase.Schema, "challenge.db")
    }
}
