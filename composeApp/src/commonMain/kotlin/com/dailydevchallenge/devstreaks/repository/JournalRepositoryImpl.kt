package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.database.JournalQueries
import com.dailydevchallenge.devstreaks.data.Journal

class JournalRepositoryImpl(
    private val journalQueries: JournalQueries
) : JournalRepository {

    override suspend fun getAllJournals(): List<Journal> {
        return journalQueries.selectAll().executeAsList().map {
            Journal(
                id = it.id,
                title = it.title,
                content = it.content,
                timestamp = it.timestamp,
                isSynced = it.isSynced.toInt() != 0
            )
        }
    }

    override suspend fun getJournalById(id: String): Journal? {
        return journalQueries.selectById(id).executeAsOneOrNull()?.let {
            Journal(
                id = it.id,
                title = it.title,
                content = it.content,
                timestamp = it.timestamp,
                isSynced = it.isSynced.toInt() != 0
            )
        }
    }

    override suspend fun addJournal(journal: Journal): Boolean {
        journalQueries.insertJournal(
            id = journal.id,
            title = journal.title,
            content = journal.content,
            timestamp = journal.timestamp,
            isSynced = if (journal.isSynced) 1 else 0
        )
        return true
    }

    override suspend fun updateJournal(journal: Journal): Boolean {
        return addJournal(journal)
    }

    override suspend fun deleteJournal(id: String): Boolean {
        journalQueries.deleteJournal(id)
        return true
    }

    override suspend fun syncPending() {
        // Placeholder: Handle syncing unsynced journals with backend if needed.
    }
}
