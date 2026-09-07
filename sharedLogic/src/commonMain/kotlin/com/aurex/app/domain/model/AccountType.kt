package com.aurex.app.domain.model

enum class AccountType {
    CASH,           // Physical cash
    BANK,           // Savings or salary accounts
    DIGITAL_WALLET, // eSewa, Khalti, PayPal
    CREDIT_CARD,    // Credit cards
    LOAN,           // Personal loans
    EMI             // Personal EMIs
}