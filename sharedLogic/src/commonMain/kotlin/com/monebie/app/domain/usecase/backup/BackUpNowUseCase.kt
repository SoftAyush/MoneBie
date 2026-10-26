package com.monebie.app.domain.usecase.backup

import com.monebie.app.domain.repository.BackupRepository
import com.monebie.app.domain.repository.RemoteBackupStore

/** What came of asking for a backup. */
sealed interface BackupOutcome {
    data class Uploaded(val rowCount: Int) : BackupOutcome

    /**
     * There was nothing worth uploading.
     *
     * Reported rather than uploaded so that a backup taken before the user has
     * entered anything cannot replace a real one taken on another device.
     */
    data object NothingToBackUp : BackupOutcome
}

/**
 * Takes a snapshot and uploads it.
 *
 * Reads first and uploads second, rather than streaming table by table, so the
 * network is never what decides how long the database is held open.
 */
class BackUpNowUseCase(
    private val backupRepository: BackupRepository,
    private val remoteBackupStore: RemoteBackupStore
) {

    suspend operator fun invoke(
        onProgress: (written: Int, total: Int) -> Unit = { _, _ -> }
    ): BackupOutcome {
        val snapshot = backupRepository.snapshot()
        if (snapshot.isEmpty) return BackupOutcome.NothingToBackUp

        remoteBackupStore.upload(snapshot, onProgress)
        return BackupOutcome.Uploaded(snapshot.totalRowCount)
    }
}
