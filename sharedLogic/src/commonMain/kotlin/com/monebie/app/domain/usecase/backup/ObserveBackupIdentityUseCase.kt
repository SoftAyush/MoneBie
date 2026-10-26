package com.monebie.app.domain.usecase.backup

import com.monebie.app.domain.repository.BackupIdentity
import com.monebie.app.domain.repository.RemoteBackupStore
import kotlinx.coroutines.flow.Flow

/**
 * Who, if anyone, backups are being kept for.
 *
 * Reads the signed-in identity, which is held locally, rather than asking
 * whether a backup actually exists in the cloud. That distinction matters for
 * the settings screen: it opens constantly, and a network round trip there
 * would either delay the whole screen or leave one row flickering between
 * states. Whether backup is switched on is a local fact and is answered
 * locally; what is stored is a question the backup screen asks, once, when the
 * user goes looking.
 */
class ObserveBackupIdentityUseCase(
    private val remoteBackupStore: RemoteBackupStore
) {
    operator fun invoke(): Flow<BackupIdentity> = remoteBackupStore.identity
}
