package com.monebie.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Resolves the domain layer's platform-neutral icon keys to Material icons.
 *
 * The domain deliberately stores a string key rather than a drawable, so this
 * mapping is the only place the two worlds meet - and iOS can map the same keys
 * to its own symbols later.
 */
object MoneBieIcons {

    fun forCategory(iconKey: String): ImageVector = when (iconKey) {
        "salary" -> Icons.Rounded.Work
        "business" -> Icons.Rounded.BusinessCenter
        "freelance" -> Icons.Rounded.Brush
        "investment" -> Icons.AutoMirrored.Rounded.TrendingUp
        "rental" -> Icons.Rounded.Apartment
        "gift" -> Icons.Rounded.CardGiftcard
        "refund" -> Icons.Rounded.Undo
        "food" -> Icons.Rounded.Restaurant
        "groceries" -> Icons.Rounded.LocalGroceryStore
        "transport" -> Icons.Rounded.DirectionsCar
        "housing" -> Icons.Rounded.Apartment
        "utilities" -> Icons.Rounded.Bolt
        "health" -> Icons.Rounded.LocalHospital
        "education" -> Icons.Rounded.School
        "shopping" -> Icons.Rounded.ShoppingBag
        "entertainment" -> Icons.Rounded.Movie
        "subscriptions" -> Icons.Rounded.EventRepeat
        "debt" -> Icons.AutoMirrored.Rounded.ReceiptLong
        "emi" -> Icons.Rounded.EventRepeat
        "lending" -> Icons.Rounded.Handshake
        "transfer" -> Icons.Rounded.SwapHoriz
        else -> Icons.Rounded.Payments
    }

    fun forAccount(iconKey: String): ImageVector = when (iconKey) {
        "cash" -> Icons.Rounded.Payments
        "bank" -> Icons.Rounded.AccountBalance
        "wallet" -> Icons.Rounded.Wallet
        "credit_card" -> Icons.Rounded.CreditCard
        "loan" -> Icons.AutoMirrored.Rounded.ReceiptLong
        "emi" -> Icons.Rounded.EventRepeat
        else -> Icons.Rounded.AccountBalanceWallet
    }

    val income: ImageVector = Icons.AutoMirrored.Rounded.TrendingUp
    val expense: ImageVector = Icons.AutoMirrored.Rounded.TrendingDown

    /**
     * The icons a user can choose from for a category of their own.
     *
     * Only keys [forCategory] actually resolves - offering one it falls through
     * on would hand the user a picture of a banknote whatever they picked.
     */
    val categoryChoices: List<String> = listOf(
        "other", "food", "groceries", "transport", "housing", "utilities",
        "health", "education", "shopping", "entertainment", "subscriptions",
        "salary", "business", "freelance", "investment", "rental", "gift",
        "refund", "lending", "debt", "emi", "transfer"
    )
}
