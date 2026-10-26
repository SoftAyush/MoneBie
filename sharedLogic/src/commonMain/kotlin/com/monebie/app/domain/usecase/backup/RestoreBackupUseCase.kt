package com.monebie.app.domain.usecase.backup

import com.monebie.app.domain.model.backup.BackupSnapshot
import com.monebie.app.domain.repository.BackupRepository
import com.monebie.app.domain.repository.RemoteBackupStore

/** What came of asking for a restore. */
sealed interface RestoreOutcome {
    data class Restored(val rowCount: Int) : RestoreOutcome

    /** This account has never uploaded a backup. */
    data object NothingStored : RestoreOutcome

    /**
     * The stored backup was never finished being written.
     *
     * Refused rather than partially applied: a ledger reassembled from an
     * arbitrary fraction of its rows would balance to the wrong number while
     * looking entirely ordinary.
     */
    data object BackupIncomplete : RestoreOutcome

    /** Written by a newer version of the app than this one can read. */
    data class FormatTooNew(val found: Int, val supported: Int) : RestoreOutcome
}

/**
 * Downloads the stored backup and puts it back.
 *
 * Every refusal is checked before a single local row is touched, so a rejected
 * restore leaves the user exactly where they were.
 */
class RestoreBackupUseCase(
    private val backupRepository: BackupRepository,
    private val remoteBackupStore: RemoteBackupStore
) {

    suspend operator fun invoke(): RestoreOutcome {
        val metadata = remoteBackupStore.latestMetadata() ?: return RestoreOutcome.NothingStored
        if (!metadata.isComplete) return RestoreOutcome.BackupIncomplete
        if (metadata.formatVersion > BackupSnapshot.CURRENT_FORMAT_VERSION) {
            return RestoreOutcome.FormatTooNew(
                found = metadata.formatVersion,
                supported = BackupSnapshot.CURRENT_FORMAT_VERSION
            )
        }

        val snapshot = remoteBackupStore.download() ?: return RestoreOutcome.NothingStored
        backupRepository.restore(snapshot)
        return RestoreOutcome.Restored(snapshot.totalRowCount)
    }
}
