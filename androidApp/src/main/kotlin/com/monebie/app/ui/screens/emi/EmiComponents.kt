package com.monebie.app.ui.screens.emi

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.monebie.app.core.MoneyFormatter
import com.monebie.app.core.formatDisplayDate
import com.monebie.app.core.relativeDueLabel
import com.monebie.app.domain.model.EmiStatus
import com.monebie.app.domain.model.displayName
import com.monebie.app.presentation.emi.EmiListItem
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.GradientTrack
import com.monebie.app.ui.components.MicroLabel
import com.monebie.app.ui.components.MoneyText
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.StatusPill
import com.monebie.app.ui.theme.MoneBieTheme

@Composable
fun emiStatusColors(status: EmiStatus): Pair<Color, Color> = when (status) {
    EmiStatus.OVERDUE -> MoneBieTheme.finance.negative to MoneBieTheme.finance.negativeContainer
    EmiStatus.COMPLETED -> MoneBieTheme.finance.positive to MoneBieTheme.finance.positiveContainer
    EmiStatus.UPCOMING -> MoneBieTheme.finance.caution to MoneBieTheme.finance.cautionContainer
    EmiStatus.ACTIVE -> MaterialTheme.colorScheme.primary to
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
}

@Composable
fun EmiStatusPill(status: EmiStatus, modifier: Modifier = Modifier) {
    val (content, container) = emiStatusColors(status)
    StatusPill(
        text = status.displayName,
        contentColor = content,
        containerColor = container,
        modifier = modifier
    )
}

/**
 * One EMI in the list.
 *
 * Leads with the monthly amount rather than the outstanding total: an EMI is a
 * recurring commitment, and what the user plans around is the payment, not the
 * balance behind it. Underneath, the term is drawn as its own installments -
 * the thing that separates an EMI from every other debt is that it ends on a
 * countable date.
 */
@Composable
fun EmiCard(
    item: EmiListItem,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emi = item.emi
    val (accent, _) = emiStatusColors(item.status)

    PremiumCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
        ) {
            GlowBadge(icon = Icons.Rounded.EventRepeat, tint = accent)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = emi.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = emi.provider,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    money = emi.monthlyAmount,
                    currencySymbol = currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    showDecimals = false
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = "per month")
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.medium))

        InstallmentStrip(
            paid = emi.installmentsPaid,
            total = emi.totalInstallments,
            accent = accent
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${emi.installmentsPaid} of ${emi.totalInstallments} paid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = MoneyFormatter.format(
                    emi.remainingAmount,
                    currencySymbol,
                    showDecimals = false
                ) + " left",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
        ) {
            EmiStatusPill(item.status)

            val days = item.daysUntilDue
            if (item.status != EmiStatus.COMPLETED && days != null) {
                // The due pill carries its own urgency rather than a flat grey:
                // "In 3d" and "Overdue by 12d" are not the same news.
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
        }
    }
}

/**
 * The term drawn as its own installments, filled as they are paid.
 *
 * A plain bar gives a fraction; this gives the schedule - you can see the
 * months you have behind you, the one that comes next, and how many are left.
 * Past a point a segment would be narrower than the gap beside it, so a very
 * long term falls back to a continuous track rather than a comb nobody can
 * count.
 */
@Composable
fun InstallmentStrip(
    paid: Int,
    total: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    /** Override on a dark surface, where an onSurface track disappears. */
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
) {
    val progress = if (total <= 0) 0f else (paid.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "installments"
    )

    if (total > MaxCountableInstallments) {
        GradientTrack(
            progress = progress,
            colors = listOf(accent.copy(alpha = 0.55f), accent),
            modifier = modifier,
            height = 6.dp
        )
        return
    }

    val nextColor = accent.copy(alpha = 0.45f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val gap = when {
            total <= 12 -> 4.dp
            total <= 24 -> 3.dp
            total <= 48 -> 2.dp
            else -> 1.5.dp
        }.toPx()

        val segment = ((size.width - gap * (total - 1)) / total).coerceAtLeast(1f)
        val radius = CornerRadius(minOf(segment, size.height) / 2f)
        val filled = fraction * total

        repeat(total) { index ->
            val left = index * (segment + gap)
            val topLeft = Offset(left, 0f)
            val segmentSize = Size(segment, size.height)

            drawRoundRect(
                color = trackColor,
                topLeft = topLeft,
                size = segmentSize,
                cornerRadius = radius
            )

            // Segments fade in one after another as the fill animates, rather
            // than the last one growing sideways out of nothing.
            val coverage = (filled - index).coerceIn(0f, 1f)
            if (coverage > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.7f), accent),
                        startX = 0f,
                        endX = size.width
                    ),
                    topLeft = topLeft,
                    size = segmentSize,
                    cornerRadius = radius,
                    alpha = coverage
                )
            } else if (index == paid) {
                // The installment that comes next, outlined rather than filled.
                drawRoundRect(
                    color = nextColor,
                    topLeft = topLeft,
                    size = segmentSize,
                    cornerRadius = radius,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

/**
 * Above this the strip stops being countable at a glance and the segments get
 * thinner than the gaps between them - six years of monthly installments.
 */
private const val MaxCountableInstallments = 72

/** One paid installment in the schedule history. */
@Composable
fun InstallmentHistoryRow(
    installmentNumber: Int,
    totalInstallments: Int,
    amount: String,
    paidDay: String,
    dueDay: String,
    accountName: String,
    isLinkedToAccount: Boolean,
    isLate: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MoneBieTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
    ) {
        GlowBadge(
            icon = Icons.Rounded.EventRepeat,
            tint = MoneBieTheme.finance.positive,
            size = 38.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Installment $installmentNumber of $totalInstallments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Paid " + formatDisplayDate(paidDay) +
                    if (isLinkedToAccount) " from $accountName" else " (recorded only)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Surfaces a payment made after its scheduled date, which the
            // running schedule alone would hide.
            if (isLate) {
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    text = "Was due " + formatDisplayDate(dueDay),
                    contentColor = MoneBieTheme.finance.caution,
                    containerColor = MoneBieTheme.finance.cautionContainer
                )
            }
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MoneBieTheme.finance.positive
        )
    }
}
