package com.aurex.app.ui.screens.debt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.core.formatDisplayDate
import com.aurex.app.core.relativeDueLabel
import com.aurex.app.domain.model.DebtStatus
import com.aurex.app.domain.model.displayName
import com.aurex.app.domain.model.perLabel
import com.aurex.app.presentation.debt.DebtListItem
import com.aurex.app.ui.components.GlassIconButton
import com.aurex.app.ui.components.GlowBadge
import com.aurex.app.ui.components.GradientTrack
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.MoneyText
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.StatusPill
import com.aurex.app.ui.theme.AurexTheme

/**
 * Colour for a debt status.
 *
 * Paid is deliberately the positive colour rather than a neutral grey: clearing
 * a debt is the outcome the module exists for, and it should read as a win.
 */
@Composable
fun statusColors(status: DebtStatus): Pair<Color, Color> = when (status) {
    DebtStatus.OVERDUE -> AurexTheme.finance.negative to AurexTheme.finance.negativeContainer
    DebtStatus.PAID -> AurexTheme.finance.positive to AurexTheme.finance.positiveContainer
    DebtStatus.PARTIALLY_PAID -> AurexTheme.finance.caution to AurexTheme.finance.cautionContainer
    DebtStatus.ACTIVE -> MaterialTheme.colorScheme.primary to
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
}

@Composable
fun DebtStatusPill(status: DebtStatus, modifier: Modifier = Modifier) {
    val (content, container) = statusColors(status)
    StatusPill(
        text = status.displayName,
        contentColor = content,
        containerColor = container,
        modifier = modifier
    )
}

/**
 * One debt in the list.
 *
 * Leads with what is still owed rather than the original amount: the remaining
 * balance is the number that changes and the one the user is tracking. The
 * status colour runs through the badge, the track and the percentage, so a row
 * can be triaged from its colour before any of it is read.
 */
@Composable
fun DebtCard(
    item: DebtListItem,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debt = item.debt
    val (accent, _) = statusColors(item.status)

    PremiumCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
        ) {
            GlowBadge(icon = Icons.AutoMirrored.Rounded.ReceiptLong, tint = accent)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = debt.lender,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    money = debt.remainingAmount,
                    currencySymbol = currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    showDecimals = false
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = "left")
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.medium))

        GradientTrack(
            progress = debt.progress,
            colors = listOf(accent.copy(alpha = 0.55f), accent),
            height = 6.dp
        )

        Spacer(Modifier.height(AurexTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = MoneyFormatter.format(debt.amountPaid, currencySymbol, showDecimals = false) +
                    " of " +
                    MoneyFormatter.format(debt.originalAmount, currencySymbol, showDecimals = false) +
                    " repaid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = MoneyFormatter.percent(debt.progress),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }

        if (item.status != DebtStatus.PAID && item.daysUntilDue != null) {
            Spacer(Modifier.height(AurexTheme.spacing.small))

            val days = item.daysUntilDue!!
            /*
             * A date near enough to act on wears a colour.
             *
             * The neutral pill said "Overdue by 12d" in the same grey as "In
             * 3mo", which is the one distinction a due date exists to make.
             */
            val dueTone = when {
                days < 0L -> AurexTheme.finance.negative
                days <= 7L -> AurexTheme.finance.caution
                else -> null
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
            ) {
                DebtStatusPill(item.status)
                StatusPill(
                    text = relativeDueLabel(days),
                    contentColor = dueTone ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = dueTone?.copy(alpha = 0.12f)
                        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                // What one payment actually costs. The card said what was left
                // and what had been repaid, but never the figure the user has
                // to find on the due date it is sitting beside.
                val installment = debt.installmentAmount?.takeIf { it.isPositive }
                if (installment != null) {
                    StatusPill(
                        text = MoneyFormatter.format(
                            installment,
                            currencySymbol,
                            showDecimals = false
                        ) + " " + debt.paymentFrequency.perLabel,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else if (item.status == DebtStatus.PAID) {
            Spacer(Modifier.height(AurexTheme.spacing.small))
            DebtStatusPill(item.status)
        }
    }
}

/** A single repayment in the debt's history. */
@Composable
fun PaymentHistoryRow(
    amount: String,
    day: String,
    accountName: String,
    note: String?,
    isLinkedToAccount: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AurexTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
    ) {
        GlowBadge(
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            tint = AurexTheme.finance.positive,
            size = 38.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDisplayDate(day),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                // Says plainly whether this payment moved a real balance, so a
                // history-only entry is never mistaken for one that did.
                text = if (isLinkedToAccount) "Paid from $accountName" else "Recorded only",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!note.isNullOrBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AurexTheme.finance.positive
        )

        Spacer(Modifier.width(AurexTheme.spacing.small))

        // Small and quiet: removing a payment rewrites the balance above it,
        // so it should be reachable without ever being the loudest thing here.
        GlassIconButton(
            icon = Icons.Rounded.DeleteOutline,
            contentDescription = "Remove this payment",
            onClick = onDelete,
            tint = MaterialTheme.colorScheme.error,
            size = 34.dp
        )
    }
}
