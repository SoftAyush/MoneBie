package com.monebie.app.ui.screens.borrowlend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
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
import com.monebie.app.core.MoneyFormatter
import com.monebie.app.core.formatDisplayDate
import com.monebie.app.core.relativeDueLabel
import com.monebie.app.domain.model.LoanDirection
import com.monebie.app.domain.model.LoanStatus
import com.monebie.app.domain.model.displayName
import com.monebie.app.presentation.borrowlend.LoanListItem
import com.monebie.app.ui.components.GlassIconButton
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.GradientTrack
import com.monebie.app.ui.components.MicroLabel
import com.monebie.app.ui.components.MoneyText
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.StatusPill
import com.monebie.app.ui.theme.MoneBieTheme

/**
 * Colour is the primary signal for direction throughout this module.
 *
 * Money the user owes uses the same negative colour as an expense; money owed
 * to them uses the distinct "receivable" colour rather than the income green,
 * so a debt owed to you never reads as money you already have.
 */
@Composable
fun directionColors(direction: LoanDirection): Pair<Color, Color> = when (direction) {
    LoanDirection.BORROWED -> MoneBieTheme.finance.negative to MoneBieTheme.finance.negativeContainer
    LoanDirection.LENT -> MoneBieTheme.finance.receivable to MoneBieTheme.finance.receivableContainer
}

@Composable
fun loanStatusColors(status: LoanStatus, direction: LoanDirection): Pair<Color, Color> =
    when (status) {
        LoanStatus.OVERDUE -> MoneBieTheme.finance.negative to MoneBieTheme.finance.negativeContainer
        LoanStatus.SETTLED -> MoneBieTheme.finance.positive to MoneBieTheme.finance.positiveContainer
        LoanStatus.PARTIALLY_SETTLED ->
            MoneBieTheme.finance.caution to MoneBieTheme.finance.cautionContainer
        LoanStatus.ACTIVE -> directionColors(direction)
    }

@Composable
fun LoanStatusPill(status: LoanStatus, direction: LoanDirection, modifier: Modifier = Modifier) {
    val (content, container) = loanStatusColors(status, direction)
    StatusPill(
        text = status.displayName,
        contentColor = content,
        containerColor = container,
        modifier = modifier
    )
}

/**
 * One borrowing or lending in the list.
 *
 * The arrow icon points the way the money went, so the direction is readable
 * before any of the text is.
 */
@Composable
fun LoanCard(
    item: LoanListItem,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val record = item.record
    val (accent, _) = directionColors(record.direction)
    val borrowed = record.direction == LoanDirection.BORROWED

    PremiumCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = if (borrowed) {
                    Icons.AutoMirrored.Rounded.CallReceived
                } else {
                    Icons.AutoMirrored.Rounded.CallMade
                },
                tint = accent
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.personName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (borrowed) "You owe them" else "They owe you",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    money = record.remainingAmount,
                    currencySymbol = currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    showDecimals = false
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = "outstanding")
            }
        }

        // Only shown once something has been settled; a full progress bar at
        // zero adds noise to what is usually a single-payment arrangement.
        if (record.amountSettled.isPositive) {
            Spacer(Modifier.height(MoneBieTheme.spacing.medium))
            GradientTrack(
                progress = record.progress,
                colors = listOf(accent.copy(alpha = 0.55f), accent),
                height = 6.dp
            )
            Spacer(Modifier.height(MoneBieTheme.spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = MoneyFormatter.format(
                        record.amountSettled,
                        currencySymbol,
                        showDecimals = false
                    ) + " of " + MoneyFormatter.format(
                        record.principalAmount,
                        currencySymbol,
                        showDecimals = false
                    ) + " settled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = MoneyFormatter.percent(record.progress),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
        ) {
            LoanStatusPill(item.status, record.direction)

            val days = item.daysUntilDue?.takeIf { item.status != LoanStatus.SETTLED }
            if (days != null) {
                // The due pill carries its own urgency: a date that has passed
                // between two people is a different thing from one still ahead.
                val dueColor = when {
                    days < 0L -> MoneBieTheme.finance.negative
                    days <= 7L -> MoneBieTheme.finance.caution
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                StatusPill(
                    text = relativeDueLabel(days),
                    contentColor = dueColor,
                    containerColor = if (days <= 7L) {
                        dueColor.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
            if (record.dueDate == null && item.status != LoanStatus.SETTLED) {
                // Common and not a problem - money between people often has no
                // date on it - so it is stated plainly rather than warned about.
                StatusPill(
                    text = "No date agreed",
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/** One repayment in a record's history. */
@Composable
fun RepaymentHistoryRow(
    amount: String,
    day: String,
    accountName: String,
    note: String?,
    isLinkedToAccount: Boolean,
    direction: LoanDirection,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borrowed = direction == LoanDirection.BORROWED
    val (accent, _) = directionColors(direction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MoneBieTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
    ) {
        GlowBadge(
            icon = if (borrowed) {
                Icons.AutoMirrored.Rounded.CallMade
            } else {
                Icons.AutoMirrored.Rounded.CallReceived
            },
            tint = accent,
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
                text = when {
                    !isLinkedToAccount -> "Recorded only"
                    borrowed -> "Paid from $accountName"
                    else -> "Received into $accountName"
                },
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
            color = accent
        )

        Spacer(Modifier.size(MoneBieTheme.spacing.small))

        // Small and quiet: removing a repayment rewrites the balance above it,
        // so it should be reachable without ever being the loudest thing here.
        GlassIconButton(
            icon = Icons.Rounded.DeleteOutline,
            contentDescription = "Remove this repayment",
            onClick = onDelete,
            tint = MaterialTheme.colorScheme.error,
            size = 34.dp
        )
    }
}
