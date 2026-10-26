package com.aurex.app.domain.model

enum class PaymentMethod {
    CASH, CARD, BANK_TRANSFER, DIGITAL_WALLET, CHEQUE, OTHER
}

val PaymentMethod.displayName: String
    get() = when (this) {
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.CARD -> "Card"
        PaymentMethod.BANK_TRANSFER -> "Bank Transfer"
        PaymentMethod.DIGITAL_WALLET -> "Digital Wallet"
        PaymentMethod.CHEQUE -> "Cheque"
        PaymentMethod.OTHER -> "Other"
    }
