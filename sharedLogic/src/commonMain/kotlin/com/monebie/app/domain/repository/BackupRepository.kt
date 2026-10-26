package com.monebie.app.domain.repository

import com.monebie.app.domain.model.backup.BackupSnapshot

/** Reads the local database out as a snapshot, and puts one back. */
interface BackupRepository {

    /** Every user-entered row, read in one consistent pass. */
    suspend fun snapshot(): BackupSnapshot

    /**
     * Replaces the local data with [snapshot].
     *
     * A replace rather than a merge. Merging would have to decide, row by row,
     * whether a local edit or the backed-up value wins, and getting that wrong
     * on a ledger means silently changing a balance the user reconciled by
     * hand. Replacing is the honest operation for a restore: the caller has
     * confirmed they want the backup's version of events.
     */
    suspend fun restore(snapshot: BackupSnapshot)
}
