package com.monebie.app.data.repository

import com.monebie.app.domain.model.backup.BackupSnapshot
import com.monebie.app.domain.repository.BackupIdentity
import com.monebie.app.domain.repository.BackupMetadata
import com.monebie.app.domain.repository.RemoteBackupStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The store used on platforms that have no cloud backend wired up yet.
 *
 * Android supplies a Firestore-backed store; iOS currently supplies nothing, and
 * this stands in for it. It reports "signed out" and refuses the actions that
 * would need a backend, which leaves the backup screen showing an honest
 * "not available here" rather than either crashing or, worse, appearing to
 * back a ledger up to somewhere that does not exist.
 *
 * Making this the default is what keeps [com.monebie.app.core.AppContainer] free
 * of a nullable store, and so keeps every caller free of null checks that would
 * otherwise exist only to describe a platform gap.
 */
object UnavailableRemoteBackupStore : RemoteBackupStore {

    private const val MESSAGE = "Cloud backup is not available on this platform yet"

    override val identity: Flow<BackupIdentity> = flowOf(BackupIdentity.SignedOut)

    override suspend fun signIn(googleIdToken: String): BackupIdentity =
        throw UnsupportedOperationException(MESSAGE)

    override suspend fun signOut() = Unit

    override suspend fun upload(
        snapshot: BackupSnapshot,
        onProgress: (written: Int, total: Int) -> Unit
    ): Unit = throw UnsupportedOperationException(MESSAGE)

    override suspend fun download(): BackupSnapshot? = null

    override suspend fun latestMetadata(): BackupMetadata? = null

    override suspend fun delete() = Unit
}
