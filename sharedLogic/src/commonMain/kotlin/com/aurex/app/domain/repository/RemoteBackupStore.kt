package com.aurex.app.domain.repository

import com.aurex.app.domain.model.backup.BackupSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Where a snapshot goes once it leaves the device.
 *
 * Declared here, in common code, with no Firebase type in sight. The Firebase
 * SDK is Android-only, so an interface is what lets the backup use cases, the
 * view model and their tests stay in `commonMain` and lets the iOS target
 * supply its own implementation later without any of that being rewritten.
 */
interface RemoteBackupStore {

    /** Who the backup belongs to, and whether anyone is signed in at all. */
    val identity: Flow<BackupIdentity>

    /**
     * Signs in with a Google account, given the ID token its sign-in flow
     * produced.
     *
     * The token is passed in rather than fetched here because obtaining one is
     * unavoidably a platform affair - on Android it means Credential Manager and
     * an Activity - and dragging that into common code would tie this interface
     * to Android for no gain. What arrives here is a plain string either
     * platform can produce.
     *
     * A real account rather than an anonymous session, and that is the whole
     * point of the feature. An anonymous account lives in the app's own storage,
     * so uninstalling - the very thing a backup exists to survive - would strand
     * the uploaded data behind an identity nothing could sign in as again. A
     * Google account is already on the user's next phone.
     */
    suspend fun signIn(googleIdToken: String): BackupIdentity

    /** Signs out. Leaves the uploaded backup where it is. */
    suspend fun signOut()

    /**
     * Uploads [snapshot], replacing whatever was there.
     *
     * [onProgress] reports rows written out of the total so a large history
     * does not look like a hung button.
     */
    suspend fun upload(snapshot: BackupSnapshot, onProgress: (written: Int, total: Int) -> Unit = { _, _ -> })

    /** The stored backup, or null when this account has never uploaded one. */
    suspend fun download(): BackupSnapshot?

    /** Describes the stored backup without pulling all of it down. */
    suspend fun latestMetadata(): BackupMetadata?

    /** Deletes the stored backup. */
    suspend fun delete()
}

/** Who the stored backup belongs to. */
sealed interface BackupIdentity {

    /** Nobody is signed in, so there is nowhere to put a backup yet. */
    data object SignedOut : BackupIdentity

    /** Signed in. [email] is what the user would type on another device. */
    data class SignedIn(
        val userId: String,
        val email: String
    ) : BackupIdentity
}

/**
 * What is known about a stored backup without downloading it.
 *
 * [isComplete] is the integrity guard. A snapshot is spread over many documents
 * and cannot be written in one atomic step, so the upload marks the backup
 * in-progress before it starts writing rows and complete only once every row
 * has landed. A backup left half-written by a dropped connection is therefore
 * visible as such, and a restore can refuse it rather than reassembling a
 * ledger from an arbitrary fraction of its rows.
 */
data class BackupMetadata(
    val createdAt: String,
    val formatVersion: Int,
    val schemaVersion: Long,
    val rowCount: Int,
    val deviceLabel: String,
    val isComplete: Boolean
)
