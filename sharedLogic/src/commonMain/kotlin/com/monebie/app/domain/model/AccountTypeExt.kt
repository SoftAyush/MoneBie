package com.monebie.app.domain.model

import com.monebie.app.core.Money

/**
 * Presentation and balance-sheet facts about [AccountType], kept outside the enum
 * so the existing account-setup flow keeps compiling untouched.
 */
val AccountType.displayName: String
    get() = when (this) {
        AccountType.CASH -> "Cash"
        AccountType.BANK -> "Bank"
        AccountType.DIGITAL_WALLET -> "Digital Wallet"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.LOAN -> "Loan"
        AccountType.EMI -> "EMI"
    }

/** Stable key resolved to an icon by the UI layer; never store an Android drawable in the domain. */
val AccountType.iconKey: String
    get() = when (this) {
        AccountType.CASH -> "cash"
        AccountType.BANK -> "bank"
        AccountType.DIGITAL_WALLET -> "wallet"
        AccountType.CREDIT_CARD -> "credit_card"
        AccountType.LOAN -> "loan"
        AccountType.EMI -> "emi"
    }

/**
 * Whether a positive balance on this account is money the user *has*.
 *
 * Credit-card, loan and EMI accounts carry an outstanding amount instead, so
 * they are summed into liabilities rather than assets. Mixing them into
 * "Total Balance" is the single most common way a finance app reports a
 * net position that is wrong by twice the card balance.
 */
val AccountType.isAsset: Boolean
    get() = when (this) {
        AccountType.CASH, AccountType.BANK, AccountType.DIGITAL_WALLET -> true
        AccountType.CREDIT_CARD, AccountType.LOAN, AccountType.EMI -> false
    }

/**
 * Which end of the signed scale a new account starts on.
 *
 * Every balance in MoneBie lives on one scale where negative means the account is
 * a claim against the user - that is what
 * [com.monebie.app.domain.model.Account.outstanding] reads, and what every
 * liability total is built from. An entry field can only offer digits, so the
 * figure a user types for a card is what they *owe*, and it belongs on the
 * other end of that scale from the figure they type for a bank account.
 */
fun openingBalanceFor(type: AccountType, entered: Money): Money =
    if (type.isAsset) entered else -entered
