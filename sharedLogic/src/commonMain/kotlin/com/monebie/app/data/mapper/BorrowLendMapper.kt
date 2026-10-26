package com.monebie.app.data.mapper

import com.monebie.app.core.Money
import com.monebie.app.domain.model.BorrowLendRecord
import com.monebie.app.domain.model.LoanDirection
import com.monebie.app.domain.model.LoanRepayment
import com.monebie.app.db.LoanRepayment as DbLoanRepayment
import com.monebie.app.db.SelectAllLoanRecords
import com.monebie.app.db.SelectLoanRecordById

fun SelectAllLoanRecords.toDomain(): BorrowLendRecord = buildRecord(
    id = id,
    personName = person_name,
    direction = direction,
    principalAmountMinor = principal_amount_minor,
    amountSettledMinor = amount_settled_minor,
    date = date,
    dueDate = due_date,
    notes = notes,
    accountId = account_id,
    principalTransactionId = principal_transaction_id
)

fun SelectLoanRecordById.toDomain(): BorrowLendRecord = buildRecord(
    id = id,
    personName = person_name,
    direction = direction,
    principalAmountMinor = principal_amount_minor,
    amountSettledMinor = amount_settled_minor,
    date = date,
    dueDate = due_date,
    notes = notes,
    accountId = account_id,
    principalTransactionId = principal_transaction_id
)

fun DbLoanRepayment.toDomain(): LoanRepayment = LoanRepayment(
    id = id,
    recordId = record_id,
    amount = Money(amount_minor),
    date = date,
    accountId = account_id,
    transactionId = transaction_id,
    note = note
)

private fun buildRecord(
    id: Long,
    personName: String,
    direction: String,
    principalAmountMinor: Long,
    amountSettledMinor: Long,
    date: String,
    dueDate: String?,
    notes: String?,
    accountId: Long?,
    principalTransactionId: Long?
): BorrowLendRecord = BorrowLendRecord(
    id = id,
    personName = personName,
    direction = direction.toDirection(),
    principalAmount = Money(principalAmountMinor),
    // Summed from the repayment history by the query, never stored.
    amountSettled = Money(amountSettledMinor),
    date = date,
    dueDate = dueDate,
    notes = notes,
    accountId = accountId,
    principalTransactionId = principalTransactionId
)

/**
 * Persisted by name. An unreadable value falls back to BORROWED rather than
 * throwing, so one bad row cannot stop the list loading.
 *
 * BORROWED is the conservative default: it counts the amount as a liability.
 * Defaulting to LENT would book an unknown as an asset and quietly overstate
 * the user's net position, which is the more dangerous way to be wrong.
 */
private fun String.toDirection(): LoanDirection =
    LoanDirection.entries.firstOrNull { it.name == this } ?: LoanDirection.BORROWED
