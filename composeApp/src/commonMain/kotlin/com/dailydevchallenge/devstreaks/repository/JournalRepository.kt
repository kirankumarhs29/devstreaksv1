package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.data.Journal

interface JournalRepository {
    suspend fun getAllJournals(): List<Journal>
    suspend fun getJournalById(id: String): Journal?
    suspend fun addJournal(journal: Journal): Boolean
    suspend fun updateJournal(journal: Journal): Boolean
    suspend fun deleteJournal(id: String): Boolean
    suspend fun syncPending()
}