package com.aurex.app.domain.usecase.account

import com.aurex.app.core.Money
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.AccountType
import com.aurex.app.domain.model.openingBalanceFor
import com.aurex.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first

/**
 * Creates an account or renames an existing one.
 *
 * The opening balance applies to creation only. An existing balance is the sum
 * of everything posted against it, so typing a new one here would silently
 * disagree with the transactions that produced it - the way to change it is to
 * record the transaction that actually moved the money.
 */
class SaveAccountUseCase(
    private val accountRepository: AccountRepository
) {

    sealed interface Result {
        data class Saved(val id: Long) : Result
        data class Invalid(val message: String) : Result
    }

    suspend operator fun invoke(
        id: Long?,
        name: String,
        type: AccountType,
        openingBalanceInput: String,
        currencyCode: String
    ): Result {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.Invalid("Give this account a name")
        if (trimmed.length > MAX_NAME_LENGTH) {
            return Result.Invalid("Keep the name under $MAX_NAME_LENGTH characters")
        }

        if (id != null) {
            accountRepository.renameAccount(id = id, name = trimmed, type = type)
            return Result.Saved(id)
        }

        // Blank is a valid "start from zero"; text that is present but
        // unparseable is reported rather than silently becoming zero, because a
        // wrong opening balance is invisible later and skews the net position
        // permanently.
        val entered = when {
            openingBalanceInput.isBlank() -> Money.ZERO
            else -> Money.parse(openingBalanceInput)
                ?: return Result.Invalid("Enter a valid opening balance")
        }

        /*
         * On a card or a loan, the figure typed is what is owed.
         *
         * Balances are stored on one signed scale, where negative means the
         * account is a claim against the user - that is what
         * [com.aurex.app.domain.model.Account.outstanding] reads, and what the
         * net position is built from. The entry field can only produce digits,
         * so a card opened at 12,000 arrived as +12,000 and was reported as
         * twelve thousand rupees *available* on the very screen that had just
         * said a card's balance counts against you.
         */
        val openingBalance = openingBalanceFor(type, entered)

        val newId = accountRepository.createAccount(
            name = trimmed,
            type = type,
            openingBalance = openingBalance,
            currencyCode = currencyCode
        )
        return Result.Saved(newId)
    }

    private companion object {
        const val MAX_NAME_LENGTH = 32
    }
}

/**
 * Archives an account.
 *
 * Archiving rather than deleting: transactions, debt payments and EMI
 * installments all point at an account id, and removing the row would strand
 * that history with nothing to name.
 */
class ArchiveAccountUseCase(
    private val accountRepository: AccountRepository
) {

    sealed interface Result {
        data object Archived : Result
        /** The app routes on having at least one account; losing the last one exits to setup. */
        data object LastAccount : Result
    }

    suspend operator fun invoke(id: Long): Result {
        val active = accountRepository.observeActiveAccounts().first()
        if (active.size <= 1) return Result.LastAccount

        accountRepository.archiveAccount(id)
        return Result.Archived
    }
}

/** One account, for an editor that opened on it. */
class GetAccountUseCase(private val accountRepository: AccountRepository) {
    suspend operator fun invoke(id: Long): Account? = accountRepository.getAccount(id)
}
