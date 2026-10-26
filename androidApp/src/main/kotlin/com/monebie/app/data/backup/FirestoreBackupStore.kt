package com.monebie.app.data.backup

import android.os.Build
import com.monebie.app.domain.model.backup.BackupAccount
import com.monebie.app.domain.model.backup.BackupBudget
import com.monebie.app.domain.model.backup.BackupRecurringTransaction
import com.monebie.app.domain.model.backup.BackupCategory
import com.monebie.app.domain.model.backup.BackupDebt
import com.monebie.app.domain.model.backup.BackupDebtPayment
import com.monebie.app.domain.model.backup.BackupEmi
import com.monebie.app.domain.model.backup.BackupEmiPayment
import com.monebie.app.domain.model.backup.BackupLoanRecord
import com.monebie.app.domain.model.backup.BackupLoanRepayment
import com.monebie.app.domain.model.backup.BackupSnapshot
import com.monebie.app.domain.model.backup.BackupTransaction
import com.monebie.app.domain.model.backup.BackupUser
import com.monebie.app.domain.repository.BackupIdentity
import com.monebie.app.domain.repository.BackupMetadata
import com.monebie.app.domain.repository.RemoteBackupStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Keeps the ledger in Cloud Firestore, one document per row.
 *
 * ### Layout
 * ```
 * users/{uid}/meta/latest          - what is stored, and whether it is whole
 * users/{uid}/profile/user         - the single profile row
 * users/{uid}/accounts/{id}        - one document per row, keyed by local id
 * users/{uid}/transactions/{id}      ... and so on for every table
 * ```
 * Keying each document by the row's own id is what makes an upload idempotent:
 * backing up twice overwrites the same documents rather than accumulating a
 * second copy of the history.
 *
 * ### Why the completeness flag
 * A snapshot spans far more than the 500 writes one Firestore batch allows, so
 * it cannot be committed atomically. The upload therefore marks the backup
 * incomplete before writing a single row and complete only after the last one
 * lands. A connection dropped midway leaves a backup that is visibly unfinished
 * instead of one that looks fine and restores a ledger missing three months of
 * transactions.
 *
 * ### Cost
 * One document per row means a backup writes as many documents as the user has
 * rows, and the stale-row sweep reads the ids already stored. That is the price
 * of a backup that is readable and queryable row by row in the console; a
 * chunked-JSON layout would cost a handful of operations instead, at the cost
 * of being opaque. Worth revisiting if a user's history ever gets large enough
 * for the free tier's daily quota to bite.
 */
class FirestoreBackupStore(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val deviceLabel: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RemoteBackupStore {

    override val identity: Flow<BackupIdentity> = callbackFlow {
        // Fires once on registration with the state as it already is, so there
        // is no separate initial read to keep in step with it.
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toIdentity()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Exchanges a Google ID token for a Firebase session.
     *
     * One call covers both a returning user and a first-time one: Firebase
     * creates the account on first sight of a Google identity, so there is no
     * separate sign-up to offer and no way for a user to end up with two
     * accounts for the same Google address.
     */
    override suspend fun signIn(googleIdToken: String): BackupIdentity =
        withContext(ioDispatcher) {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            auth.signInWithCredential(credential).await().user.toIdentity()
        }

    override suspend fun signOut() = withContext(ioDispatcher) {
        auth.signOut()
    }

    override suspend fun upload(
        snapshot: BackupSnapshot,
        onProgress: (written: Int, total: Int) -> Unit
    ) {
        withContext(ioDispatcher) {
            val root = requireUserDocument()
            val total = snapshot.totalRowCount
            var written = 0
            onProgress(0, total)

            val count = { added: Int ->
                written += added
                onProgress(written, total)
            }

            root.metaDocument().set(snapshot.toMetadataMap(complete = false)).await()

            // The profile is a collection of exactly one document rather than a
            // field on the user document, so that every table is read and written
            // the same way and the meta document stays small and cheap to poll.
            replaceCollection(
                collection = root.collection(PROFILE),
                documents = snapshot.user?.let { mapOf(USER_DOCUMENT to it.toMap()) } ?: emptyMap(),
                onWritten = count
            )
            replaceCollection(root.collection(CATEGORIES), snapshot.categories.associate { it.id to it.toMap() }, count)
            replaceCollection(root.collection(ACCOUNTS), snapshot.accounts.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(DEBTS), snapshot.debts.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(DEBT_PAYMENTS), snapshot.debtPayments.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(EMIS), snapshot.emis.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(EMI_PAYMENTS), snapshot.emiPayments.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(LOAN_RECORDS), snapshot.loanRecords.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(LOAN_REPAYMENTS), snapshot.loanRepayments.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(TRANSACTIONS), snapshot.transactions.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(BUDGETS), snapshot.budgets.associate { it.id.toString() to it.toMap() }, count)
            replaceCollection(root.collection(RECURRING), snapshot.recurringTransactions.associate { it.id.toString() to it.toMap() }, count)

            root.metaDocument().set(snapshot.toMetadataMap(complete = true)).await()
        }
    }

    override suspend fun download(): BackupSnapshot? = withContext(ioDispatcher) {
        val root = requireUserDocument()
        val meta = root.metaDocument().get(Source.SERVER).await()
        if (!meta.exists()) return@withContext null

        BackupSnapshot(
            formatVersion = meta.getLong(FIELD_FORMAT_VERSION)?.toInt()
                ?: BackupSnapshot.CURRENT_FORMAT_VERSION,
            schemaVersion = meta.getLong(FIELD_SCHEMA_VERSION) ?: 0L,
            createdAt = meta.getString(FIELD_CREATED_AT).orEmpty(),
            user = root.collection(PROFILE).get(Source.SERVER).await().documents.firstOrNull()?.toBackupUser(),
            accounts = root.collection(ACCOUNTS).documents().map { it.toBackupAccount() },
            categories = root.collection(CATEGORIES).documents().map { it.toBackupCategory() },
            transactions = root.collection(TRANSACTIONS).documents().map { it.toBackupTransaction() },
            debts = root.collection(DEBTS).documents().map { it.toBackupDebt() },
            debtPayments = root.collection(DEBT_PAYMENTS).documents().map { it.toBackupDebtPayment() },
            emis = root.collection(EMIS).documents().map { it.toBackupEmi() },
            emiPayments = root.collection(EMI_PAYMENTS).documents().map { it.toBackupEmiPayment() },
            loanRecords = root.collection(LOAN_RECORDS).documents().map { it.toBackupLoanRecord() },
            loanRepayments = root.collection(LOAN_REPAYMENTS).documents().map { it.toBackupLoanRepayment() },
            budgets = root.collection(BUDGETS).documents().map { it.toBackupBudget() },
            recurringTransactions = root.collection(RECURRING).documents().map { it.toBackupRecurringTransaction() }
        )
    }

    override suspend fun latestMetadata(): BackupMetadata? = withContext(ioDispatcher) {
        val meta = requireUserDocument().metaDocument().get(Source.SERVER).await()
        if (!meta.exists()) return@withContext null

        BackupMetadata(
            createdAt = meta.getString(FIELD_CREATED_AT).orEmpty(),
            formatVersion = meta.getLong(FIELD_FORMAT_VERSION)?.toInt() ?: 0,
            schemaVersion = meta.getLong(FIELD_SCHEMA_VERSION) ?: 0L,
            rowCount = meta.getLong(FIELD_ROW_COUNT)?.toInt() ?: 0,
            deviceLabel = meta.getString(FIELD_DEVICE_LABEL).orEmpty(),
            // Absent rather than false on a document written by an older build:
            // treating "not stated" as complete would let exactly the
            // half-written backup this flag exists to catch through.
            isComplete = meta.getBoolean(FIELD_COMPLETE) == true
        )
    }

    /**
     * Removes the stored backup.
     *
     * The meta document goes first. Everything after it is a best-effort sweep
     * of the rows, and if that is interrupted what remains is an orphaned set of
     * rows with nothing pointing at them - which reads as "no backup" and will
     * be overwritten by the next upload, rather than as a backup that can be
     * restored from.
     */
    override suspend fun delete() = withContext(ioDispatcher) {
        val root = requireUserDocument()
        root.metaDocument().delete().await()
        ALL_ROW_COLLECTIONS.forEach { name ->
            replaceCollection(root.collection(name), emptyMap()) {}
        }
    }

    /**
     * Makes [collection] hold exactly [documents].
     *
     * Rows already stored under an id that is no longer present are deleted, so
     * a transaction the user removed on this device does not come back on the
     * next restore.
     */
    private suspend fun replaceCollection(
        collection: CollectionReference,
        documents: Map<String, Map<String, Any?>>,
        onWritten: (Int) -> Unit
    ) {
        val storedIds = collection.get(Source.SERVER).await().documents.map { it.id }.toSet()

        (storedIds - documents.keys).chunked(BATCH_LIMIT).forEach { staleIds ->
            val batch = firestore.batch()
            staleIds.forEach { batch.delete(collection.document(it)) }
            batch.commit().await()
        }

        documents.entries.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (id, fields) -> batch.set(collection.document(id), fields) }
            batch.commit().await()
            onWritten(chunk.size)
        }
    }

    /**
     * Reads a collection from the server, never the local cache.
     *
     * Firestore keeps offline persistence on by default and will happily answer
     * a plain `get()` from cache. For a restore that would mean quietly putting
     * back a stale copy of the ledger, and for the stale-row sweep it would mean
     * failing to notice rows that are no longer in the backup - so both would
     * look like they had worked.
     */
    private suspend fun CollectionReference.documents(): List<DocumentSnapshot> =
        get(Source.SERVER).await().documents

    private fun requireUserDocument(): DocumentReference {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Sign in before backing up or restoring")
        return firestore.collection(USERS).document(uid)
    }

    private fun DocumentReference.metaDocument(): DocumentReference =
        collection(META).document(LATEST)

    private fun BackupSnapshot.toMetadataMap(complete: Boolean): Map<String, Any?> = mapOf(
        FIELD_CREATED_AT to createdAt,
        FIELD_FORMAT_VERSION to formatVersion.toLong(),
        FIELD_SCHEMA_VERSION to schemaVersion,
        FIELD_ROW_COUNT to totalRowCount.toLong(),
        FIELD_DEVICE_LABEL to deviceLabel,
        FIELD_COMPLETE to complete
    )

    private fun FirebaseUser?.toIdentity(): BackupIdentity = when (this) {
        null -> BackupIdentity.SignedOut
        else -> BackupIdentity.SignedIn(userId = uid, email = email.orEmpty())
    }

    private companion object {
        /** Firestore commits at most 500 operations per batch. */
        const val BATCH_LIMIT = 400

        const val USERS = "users"
        const val META = "meta"
        const val LATEST = "latest"
        const val PROFILE = "profile"
        const val USER_DOCUMENT = "user"
        const val ACCOUNTS = "accounts"
        const val CATEGORIES = "categories"
        const val TRANSACTIONS = "transactions"
        const val DEBTS = "debts"
        const val DEBT_PAYMENTS = "debtPayments"
        const val EMIS = "emis"
        const val EMI_PAYMENTS = "emiPayments"
        const val LOAN_RECORDS = "loanRecords"
        const val LOAN_REPAYMENTS = "loanRepayments"
        const val BUDGETS = "budgets"
        const val RECURRING = "recurringTransactions"

        val ALL_ROW_COLLECTIONS = listOf(
            PROFILE, ACCOUNTS, CATEGORIES, TRANSACTIONS, DEBTS, DEBT_PAYMENTS,
            EMIS, EMI_PAYMENTS, LOAN_RECORDS, LOAN_REPAYMENTS, BUDGETS, RECURRING
        )

        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_FORMAT_VERSION = "formatVersion"
        const val FIELD_SCHEMA_VERSION = "schemaVersion"
        const val FIELD_ROW_COUNT = "rowCount"
        const val FIELD_DEVICE_LABEL = "deviceLabel"
        const val FIELD_COMPLETE = "complete"
    }
}

// ---------------------------------------------------------------------------
// Row <-> document mapping
//
// Written by hand rather than through Firestore's object mapper. The mapper
// needs a no-argument constructor and mutable fields, which would mean giving
// every backup model a second, laxer shape purely for serialisation - and a
// silently defaulted field in a financial row is a wrong balance, not a missing
// label. Reading is strict for the same reason: a row missing a required field
// throws rather than restoring a zero.
// ---------------------------------------------------------------------------

private fun BackupUser.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "date" to date,
    "currencyCode" to currencyCode,
    "themeMode" to themeMode
)

private fun BackupAccount.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "type" to type,
    "balanceMinor" to balanceMinor,
    "currencyCode" to currencyCode,
    "isArchived" to isArchived,
    "createdAt" to createdAt
)

private fun BackupCategory.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "iconKey" to iconKey,
    "appliesTo" to appliesTo,
    "isBuiltIn" to isBuiltIn,
    "isProtected" to isProtected,
    "isArchived" to isArchived,
    "sortOrder" to sortOrder,
    "createdAt" to createdAt
)

private fun BackupTransaction.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "amountMinor" to amountMinor,
    "type" to type,
    "categoryId" to categoryId,
    "accountId" to accountId,
    "transferAccountId" to transferAccountId,
    "date" to date,
    "note" to note,
    "paymentMethod" to paymentMethod,
    "sourceType" to sourceType,
    "sourceReferenceId" to sourceReferenceId,
    "sourcePaymentId" to sourcePaymentId
)

private fun BackupDebt.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "lender" to lender,
    "originalAmountMinor" to originalAmountMinor,
    "interestRate" to interestRate,
    "startDate" to startDate,
    "dueDate" to dueDate,
    "paymentFrequency" to paymentFrequency,
    "installmentAmountMinor" to installmentAmountMinor,
    "notes" to notes,
    "defaultAccountId" to defaultAccountId,
    "createdAt" to createdAt
)

private fun BackupDebtPayment.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "debtId" to debtId,
    "amountMinor" to amountMinor,
    "date" to date,
    "accountId" to accountId,
    "transactionId" to transactionId,
    "note" to note
)

private fun BackupEmi.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "provider" to provider,
    "principalAmountMinor" to principalAmountMinor,
    "totalPayableAmountMinor" to totalPayableAmountMinor,
    "monthlyAmountMinor" to monthlyAmountMinor,
    "interestRate" to interestRate,
    "startDate" to startDate,
    "tenureYears" to tenureYears,
    "paymentAccountId" to paymentAccountId,
    "notes" to notes,
    "createdAt" to createdAt
)

private fun BackupEmiPayment.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "emiId" to emiId,
    "installmentNumber" to installmentNumber,
    "amountMinor" to amountMinor,
    "dueDate" to dueDate,
    "paidDate" to paidDate,
    "accountId" to accountId,
    "transactionId" to transactionId
)

private fun BackupLoanRecord.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "personName" to personName,
    "direction" to direction,
    "principalAmountMinor" to principalAmountMinor,
    "date" to date,
    "dueDate" to dueDate,
    "notes" to notes,
    "accountId" to accountId,
    "principalTransactionId" to principalTransactionId,
    "createdAt" to createdAt
)

private fun BackupLoanRepayment.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "recordId" to recordId,
    "amountMinor" to amountMinor,
    "date" to date,
    "accountId" to accountId,
    "transactionId" to transactionId,
    "note" to note
)

private fun DocumentSnapshot.toBackupUser() = BackupUser(
    name = requireString("name"),
    date = requireString("date"),
    currencyCode = requireString("currencyCode"),
    themeMode = requireString("themeMode")
)

private fun DocumentSnapshot.toBackupAccount() = BackupAccount(
    id = requireLong("id"),
    name = requireString("name"),
    type = requireString("type"),
    balanceMinor = requireLong("balanceMinor"),
    currencyCode = requireString("currencyCode"),
    isArchived = requireLong("isArchived"),
    createdAt = requireString("createdAt")
)

private fun DocumentSnapshot.toBackupCategory() = BackupCategory(
    id = requireString("id"),
    name = requireString("name"),
    iconKey = requireString("iconKey"),
    appliesTo = requireString("appliesTo"),
    isBuiltIn = requireLong("isBuiltIn"),
    isProtected = requireLong("isProtected"),
    isArchived = requireLong("isArchived"),
    sortOrder = requireLong("sortOrder"),
    createdAt = requireString("createdAt")
)

private fun DocumentSnapshot.toBackupTransaction() = BackupTransaction(
    id = requireLong("id"),
    amountMinor = requireLong("amountMinor"),
    type = requireString("type"),
    categoryId = requireString("categoryId"),
    accountId = requireLong("accountId"),
    transferAccountId = getLong("transferAccountId"),
    date = requireString("date"),
    note = getString("note"),
    paymentMethod = requireString("paymentMethod"),
    sourceType = requireString("sourceType"),
    sourceReferenceId = getLong("sourceReferenceId"),
    sourcePaymentId = getLong("sourcePaymentId")
)

private fun DocumentSnapshot.toBackupDebt() = BackupDebt(
    id = requireLong("id"),
    name = requireString("name"),
    lender = requireString("lender"),
    originalAmountMinor = requireLong("originalAmountMinor"),
    interestRate = getDouble("interestRate"),
    startDate = requireString("startDate"),
    dueDate = getString("dueDate"),
    paymentFrequency = requireString("paymentFrequency"),
    installmentAmountMinor = getLong("installmentAmountMinor"),
    notes = getString("notes"),
    defaultAccountId = getLong("defaultAccountId"),
    createdAt = requireString("createdAt")
)

private fun DocumentSnapshot.toBackupDebtPayment() = BackupDebtPayment(
    id = requireLong("id"),
    debtId = requireLong("debtId"),
    amountMinor = requireLong("amountMinor"),
    date = requireString("date"),
    accountId = getLong("accountId"),
    transactionId = getLong("transactionId"),
    note = getString("note")
)

private fun DocumentSnapshot.toBackupEmi() = BackupEmi(
    id = requireLong("id"),
    name = requireString("name"),
    provider = requireString("provider"),
    principalAmountMinor = requireLong("principalAmountMinor"),
    totalPayableAmountMinor = requireLong("totalPayableAmountMinor"),
    monthlyAmountMinor = requireLong("monthlyAmountMinor"),
    interestRate = getDouble("interestRate"),
    startDate = requireString("startDate"),
    tenureYears = requireDouble("tenureYears"),
    paymentAccountId = getLong("paymentAccountId"),
    notes = getString("notes"),
    createdAt = requireString("createdAt")
)

private fun DocumentSnapshot.toBackupEmiPayment() = BackupEmiPayment(
    id = requireLong("id"),
    emiId = requireLong("emiId"),
    installmentNumber = requireLong("installmentNumber"),
    amountMinor = requireLong("amountMinor"),
    dueDate = requireString("dueDate"),
    paidDate = requireString("paidDate"),
    accountId = getLong("accountId"),
    transactionId = getLong("transactionId")
)

private fun DocumentSnapshot.toBackupLoanRecord() = BackupLoanRecord(
    id = requireLong("id"),
    personName = requireString("personName"),
    direction = requireString("direction"),
    principalAmountMinor = requireLong("principalAmountMinor"),
    date = requireString("date"),
    dueDate = getString("dueDate"),
    notes = getString("notes"),
    accountId = getLong("accountId"),
    principalTransactionId = getLong("principalTransactionId"),
    createdAt = requireString("createdAt")
)

private fun DocumentSnapshot.toBackupLoanRepayment() = BackupLoanRepayment(
    id = requireLong("id"),
    recordId = requireLong("recordId"),
    amountMinor = requireLong("amountMinor"),
    date = requireString("date"),
    accountId = getLong("accountId"),
    transactionId = getLong("transactionId"),
    note = getString("note")
)

private fun DocumentSnapshot.requireString(field: String): String =
    getString(field) ?: throw IllegalStateException(missing(field))

private fun DocumentSnapshot.requireLong(field: String): Long =
    getLong(field) ?: throw IllegalStateException(missing(field))

private fun DocumentSnapshot.requireDouble(field: String): Double =
    getDouble(field) ?: throw IllegalStateException(missing(field))

private fun DocumentSnapshot.missing(field: String) =
    "The backup is damaged: ${reference.path} has no '$field'"

private fun BackupBudget.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "categoryId" to categoryId,
    "amountMinor" to amountMinor,
    "period" to period,
    "createdAt" to createdAt
)

private fun BackupRecurringTransaction.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "amountMinor" to amountMinor,
    "type" to type,
    "categoryId" to categoryId,
    "accountId" to accountId,
    "transferAccountId" to transferAccountId,
    "paymentMethod" to paymentMethod,
    "frequency" to frequency,
    "startDate" to startDate,
    "nextDueDate" to nextDueDate,
    "endDate" to endDate,
    "note" to note,
    "isPaused" to isPaused,
    "createdAt" to createdAt
)

private fun DocumentSnapshot.toBackupBudget() = BackupBudget(
    id = requireLong("id"),
    categoryId = requireString("categoryId"),
    amountMinor = requireLong("amountMinor"),
    period = requireString("period"),
    createdAt = requireString("createdAt")
)

private fun DocumentSnapshot.toBackupRecurringTransaction() = BackupRecurringTransaction(
    id = requireLong("id"),
    name = requireString("name"),
    amountMinor = requireLong("amountMinor"),
    type = requireString("type"),
    categoryId = requireString("categoryId"),
    accountId = requireLong("accountId"),
    transferAccountId = getLong("transferAccountId"),
    paymentMethod = requireString("paymentMethod"),
    frequency = requireString("frequency"),
    startDate = requireString("startDate"),
    nextDueDate = requireString("nextDueDate"),
    endDate = getString("endDate"),
    note = getString("note"),
    isPaused = requireLong("isPaused"),
    createdAt = requireString("createdAt")
)
