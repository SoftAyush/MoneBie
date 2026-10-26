package com.monebie.app.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.monebie.app.core.Money
import com.monebie.app.domain.model.BudgetLevel
import com.monebie.app.domain.model.BudgetStatus
import com.monebie.app.core.MoneyFormatter
import com.monebie.app.core.AppCalendar
import com.monebie.app.core.BsDate
import com.monebie.app.core.CalendarSystem
import com.monebie.app.core.NepaliCalendar
import com.monebie.app.core.formatDisplayDate
import com.monebie.app.core.formatWeekday
import com.monebie.app.core.relativeDueLabel
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.DailyTotal
import com.monebie.app.domain.model.DashboardSnapshot
import com.monebie.app.domain.model.NetPosition
import com.monebie.app.domain.model.PeriodSummary
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionCategory
import com.monebie.app.domain.model.TransactionSourceType
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.model.UpcomingPayment
import com.monebie.app.domain.model.UpcomingPaymentKind
import com.monebie.app.domain.model.displayName
import com.monebie.app.domain.model.iconKey
import com.monebie.app.ui.components.MoneBieIcons
import com.monebie.app.ui.components.CardDivider
import com.monebie.app.ui.components.CountUpMoneyText
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.GradientTrack
import com.monebie.app.ui.components.HeroCardShape
import com.monebie.app.ui.components.HeroDecoration
import com.monebie.app.ui.components.HeroFinance
import com.monebie.app.ui.components.HeroIconButton
import com.monebie.app.ui.components.HeroSplitBar
import com.monebie.app.ui.components.HeroTile
import com.monebie.app.ui.components.MicroLabel
import com.monebie.app.ui.components.OnHero
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.PremiumSectionHeader
import com.monebie.app.ui.components.heroBrush
import com.monebie.app.ui.components.maskedValue
import com.monebie.app.ui.components.pressable
import com.monebie.app.ui.components.EmptyState
import com.monebie.app.ui.components.MoneyText
import com.monebie.app.ui.components.StatusPill
import com.monebie.app.ui.components.brandBrush
import com.monebie.app.ui.components.onBrandColor
import com.monebie.app.ui.components.signedMoneyColor
import com.monebie.app.ui.theme.MoneBieTheme
import com.monebie.app.ui.theme.Palette700
import com.monebie.app.ui.theme.Palette900

// -----------------------------------------------------------------------------
// Header
// -----------------------------------------------------------------------------

/**
 * The greeting line.
 *
 * The avatar is the way into the profile rather than a second icon button: it
 * is the one element on the screen that is about the person rather than their
 * money, so it earns the distinct shape.
 */
@Composable
fun DashboardHeader(
    greeting: String,
    userName: String,
    onOpenAccounts: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(userName = userName, onClick = onOpenProfile)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            MicroLabel(text = greeting, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(
                text = userName.ifBlank { "Welcome back" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HeaderAction(
            icon = Icons.Rounded.AccountBalanceWallet,
            contentDescription = "Accounts",
            onClick = onOpenAccounts
        )
    }
}

/** The user's initial on the brand fill, ringed so it reads as a portrait slot. */
@Composable
private fun ProfileAvatar(
    userName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = userName.trim().firstOrNull()?.uppercase() ?: "A"

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(brandBrush())
            .pressable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = onBrandColor()
        )
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), CircleShape)
            .pressable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// Hero: total balance and net position
// -----------------------------------------------------------------------------

/**
 * The one card the user checks daily.
 *
 * Total Balance is the headline because it answers "what do I have right now".
 * The net position sits directly beneath it because a balance alone is
 * misleading when there are debts and EMIs behind it - the whole point of this
 * application - and the two supporting tiles show the sides that produced it.
 */
@Composable
fun BalanceHeroCard(
    totalBalance: Money,
    netPosition: NetPosition,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var masked by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = HeroCardShape,
                ambientColor = Palette900,
                spotColor = Palette700
            )
            .clip(HeroCardShape)
            .background(heroBrush())
            .pressable(onClick)
    ) {
        HeroDecoration(Modifier.matchParentSize())

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicroLabel(text = "Total balance", color = OnHero.copy(alpha = 0.75f))
                Spacer(Modifier.weight(1f))
                HeroIconButton(
                    icon = if (masked) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (masked) "Show balance" else "Hide balance",
                    onClick = { masked = !masked }
                )
            }

            Spacer(Modifier.height(10.dp))

            CountUpMoneyText(
                money = totalBalance,
                currencySymbol = currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = OnHero,
                masked = masked
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                NetPositionPill(
                    netPosition = netPosition,
                    currencySymbol = currencySymbol,
                    masked = masked
                )
                Spacer(Modifier.weight(1f))

                // A frosted pill rather than a line of text: the whole card
                // opens the balance sheet, and this is the mark that says so.
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .padding(start = 12.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Balance sheet",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnHero.copy(alpha = 0.90f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = OnHero.copy(alpha = 0.90f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            /*
             * The two sides as one bar.
             *
             * The net figure above says which way the position falls; this says
             * by how far, before either tile is read - a left band short of the
             * tick is a balance sheet the debts are winning. Dropped while the
             * balance is hidden: the proportion is as private as the figures.
             */
            if (!masked) {
                Spacer(Modifier.height(16.dp))

                HeroSplitBar(
                    leftValue = netPosition.assets.total.minorUnits,
                    rightValue = netPosition.liabilities.total.minorUnits,
                    leftColor = HeroFinance.positive,
                    rightColor = HeroFinance.negative
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Assets",
                    value = if (masked) {
                        maskedValue()
                    } else {
                        MoneyFormatter.compact(netPosition.assets.total, currencySymbol)
                    },
                    // Coloured to match its band above, so the bar needs no
                    // legend of its own.
                    valueColor = if (masked) OnHero else HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Liabilities",
                    value = if (masked) {
                        maskedValue()
                    } else {
                        MoneyFormatter.compact(netPosition.liabilities.total, currencySymbol)
                    },
                    valueColor = if (masked) OnHero else HeroFinance.negative,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** The net position, the figure the whole app exists to keep honest. */
@Composable
private fun NetPositionPill(
    netPosition: NetPosition,
    currencySymbol: String,
    masked: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (netPosition.isPositive) HeroFinance.positive else HeroFinance.negative

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (netPosition.isPositive) {
                Icons.Rounded.ArrowUpward
            } else {
                Icons.Rounded.ArrowDownward
            },
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Net " + if (masked) {
                maskedValue()
            } else {
                MoneyFormatter.compact(netPosition.net, currencySymbol)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

// -----------------------------------------------------------------------------
// Quick actions
// -----------------------------------------------------------------------------

/**
 * The four things a user opens this screen to do.
 *
 * Recording money is two taps from anywhere else in the app; from here it is
 * one. Insights and reminders sit beside them because they are the two screens
 * with no tab of their own.
 */
@Composable
fun QuickActionsRow(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickAction(
            label = "Income",
            icon = Icons.Rounded.Add,
            tint = MoneBieTheme.finance.positive,
            onClick = onAddIncome,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            label = "Expense",
            icon = Icons.Rounded.Remove,
            tint = MoneBieTheme.finance.negative,
            onClick = onAddExpense,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            label = "Insights",
            icon = Icons.Rounded.PieChart,
            tint = MaterialTheme.colorScheme.primary,
            onClick = onOpenInsights,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            label = "Reminders",
            icon = Icons.Rounded.NotificationsNone,
            tint = MoneBieTheme.finance.caution,
            onClick = onOpenReminders,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(shape)
            // Washed in what the action does rather than left neutral: four
            // grey boxes make the user read four labels to tell them apart.
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = 0.12f), scheme.surface.copy(alpha = 0.92f))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.22f), shape)
            .pressable(onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlowBadge(icon = icon, tint = tint, size = 38.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// -----------------------------------------------------------------------------
// Today
// -----------------------------------------------------------------------------

/**
 * How today is going, against how a day usually goes.
 *
 * It used to be an income figure and an expense figure side by side. On most
 * days one of those two was a zero taking half the card, and neither said
 * anything a reader could act on: 250 spent is only meaningful next to what a
 * day normally costs. The month card below already carries income against
 * expenses for the period, so repeating the pair here bought nothing.
 *
 * Money in keeps a place, as a pill, on the days it actually happens - it just
 * no longer holds a column open on the days it does not.
 */
@Composable
fun TodayCard(
    summary: PeriodSummary,
    usualDailyExpense: Money,
    today: String,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val spent = summary.expense

    PremiumCard(modifier = modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MicroLabel(text = "Today")
            Spacer(Modifier.weight(1f))
            // The card never said which day it was describing.
            Text(
                text = formatWeekday(today) + " · " + formatDisplayDate(today),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (summary.hasActivity) {
                    MoneyText(
                        money = spent,
                        currencySymbol = currencySymbol,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (spent.isZero) scheme.onSurface else MoneBieTheme.finance.negative,
                        showDecimals = false
                    )
                    Spacer(Modifier.height(2.dp))
                    MicroLabel(text = "spent")
                } else {
                    Text(
                        text = "Nothing yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    MicroLabel(text = "no entries today")
                }
            }

            // Only on the days it happened, rather than a permanent zero.
            if (!summary.income.isZero) {
                Spacer(Modifier.width(MoneBieTheme.spacing.small))
                StatusPill(
                    text = "+" + MoneyFormatter.format(
                        summary.income,
                        currencySymbol,
                        showDecimals = false
                    ) + " in",
                    contentColor = MoneBieTheme.finance.positive,
                    containerColor = MoneBieTheme.finance.positive.copy(alpha = 0.12f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        UsualDayComparison(
            spent = spent,
            usual = usualDailyExpense,
            hasActivity = summary.hasActivity,
            currencySymbol = currencySymbol
        )
    }
}

/**
 * Today's spending against a usual day, in a sentence.
 *
 * Spending less than usual is the positive colour and spending more is the
 * caution one, never the negative: an expensive day is worth noticing, but it
 * is not an error, and the colour reserved for money owed should not be spent
 * on it.
 */
@Composable
private fun UsualDayComparison(
    spent: Money,
    usual: Money,
    hasActivity: Boolean,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val usualText = MoneyFormatter.format(usual, currencySymbol, showDecimals = false)

    // A tenth of a usual day, so a few rupees either way still reads as normal.
    val band = Money(usual.minorUnits / 10L)
    val delta = spent - usual

    data class Line(val icon: ImageVector, val text: String, val tint: Color)

    val line = when {
        usual.isZero -> Line(
            Icons.Rounded.Insights,
            "No spending in the last week to compare with",
            scheme.onSurfaceVariant
        )
        !hasActivity -> Line(
            Icons.Rounded.Insights,
            "A usual day costs $usualText",
            scheme.onSurfaceVariant
        )
        delta.absolute <= band -> Line(
            Icons.Rounded.Insights,
            "About what a day usually costs ($usualText)",
            scheme.onSurfaceVariant
        )
        delta.isNegative -> Line(
            Icons.Rounded.ArrowDownward,
            MoneyFormatter.format(delta.absolute, currencySymbol, showDecimals = false) +
                " less than a usual day ($usualText)",
            MoneBieTheme.finance.positive
        )
        else -> Line(
            Icons.Rounded.ArrowUpward,
            MoneyFormatter.format(delta, currencySymbol, showDecimals = false) +
                " more than a usual day ($usualText)",
            MoneBieTheme.finance.caution
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = line.icon,
            contentDescription = null,
            tint = line.tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodySmall,
            color = line.tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** One labelled money figure with its direction marked by an arrow, not colour alone. */
@Composable
private fun FlowFigure(
    label: String,
    money: Money,
    currencySymbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(3.dp))
        MoneyText(
            money = money,
            currencySymbol = currencySymbol,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            showDecimals = false
        )
    }
}

/** Hairline between two figures sharing a row. */
@Composable
private fun FigureRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 14.dp)
            .size(width = 1.dp, height = 34.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    )
}

// -----------------------------------------------------------------------------
// This month
// -----------------------------------------------------------------------------

@Composable
fun MonthOverviewCard(
    summary: PeriodSummary,
    trend: List<DailyTotal>,
    monthLabel: String,
    today: String,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val overspending = summary.net.isNegative

    PremiumCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(text = "This month")
                Spacer(Modifier.height(3.dp))
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                text = if (overspending) "Overspending" else "On track",
                contentColor = if (overspending) {
                    MoneBieTheme.finance.negative
                } else {
                    MoneBieTheme.finance.positive
                },
                containerColor = if (overspending) {
                    MoneBieTheme.finance.negativeContainer
                } else {
                    MoneBieTheme.finance.positiveContainer
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowFigure(
                label = "Income",
                money = summary.income,
                currencySymbol = currencySymbol,
                color = MoneBieTheme.finance.positive,
                icon = Icons.Rounded.ArrowUpward,
                modifier = Modifier.weight(1f)
            )
            FigureRule()
            FlowFigure(
                label = "Expenses",
                money = summary.expense,
                currencySymbol = currencySymbol,
                color = MoneBieTheme.finance.negative,
                icon = Icons.Rounded.ArrowDownward,
                modifier = Modifier.weight(1f)
            )
            FigureRule()
            FlowFigure(
                label = "Net",
                money = summary.net,
                currencySymbol = currencySymbol,
                color = signedMoneyColor(summary.net),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))

        // Expense as a share of income: the fastest read on whether a month is healthy.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MicroLabel(text = "Spent of income")
            Spacer(Modifier.weight(1f))
            Text(
                text = if (summary.income.isZero) "-" else MoneyFormatter.percent(summary.expenseRatio),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (summary.expenseRatio > 0.85f) {
                    MoneBieTheme.finance.negative
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        GradientTrack(
            progress = summary.expenseRatio,
            colors = if (summary.expenseRatio > 0.85f) {
                listOf(MoneBieTheme.finance.caution, MoneBieTheme.finance.negative)
            } else {
                listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
            }
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (summary.income.isZero) {
                "No income recorded this month yet"
            } else {
                MoneyFormatter.format(summary.income - summary.expense, currencySymbol, showDecimals = false, signed = true) +
                    " left of what came in"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (trend.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            CardDivider()
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MicroLabel(text = "Last 7 days")
                Spacer(Modifier.weight(1f))
                LegendDot(color = MoneBieTheme.finance.positive, label = "In")
                Spacer(Modifier.width(12.dp))
                LegendDot(color = MoneBieTheme.finance.negative, label = "Out")
            }

            Spacer(Modifier.height(12.dp))

            // Seven flat stubs say nothing a sentence cannot say better, and a
            // highlighted column with no bar in it looks like a rendering fault.
            if (trend.any { it.income.minorUnits > 0L || it.expense.minorUnits > 0L }) {
                WeeklyTrendChart(trend = trend, today = today)
            } else {
                Text(
                    text = "No money moved in the last seven days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Seven days of income and expense.
 *
 * Both series share one scale, so the two colours are directly comparable and a
 * heavy spending day is obvious at a glance. Today gets a tinted column behind
 * it, which is what turns the chart from a picture of a week into a position in
 * one. Bars are laid out rather than drawn on a Canvas so they inherit theme
 * colours and stay crisp at any density.
 */
@Composable
private fun WeeklyTrendChart(
    trend: List<DailyTotal>,
    today: String,
    modifier: Modifier = Modifier
) {
    val peak = trend.maxOf { maxOf(it.income.minorUnits, it.expense.minorUnits) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        trend.forEach { day ->
            val isToday = day.day == today

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isToday) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom
                ) {
                    TrendBar(
                        value = day.income.minorUnits,
                        peak = peak,
                        color = MoneBieTheme.finance.positive
                    )
                    TrendBar(
                        value = day.expense.minorUnits,
                        peak = peak,
                        color = MoneBieTheme.finance.negative
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = formatWeekday(day.day),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RowScope.TrendBar(value: Long, peak: Long, color: Color) {
    // A floor of 4% keeps a zero day visible as a baseline tick rather than vanishing.
    val target = if (peak <= 0L) 0.04f else (value.toFloat() / peak.toFloat()).coerceIn(0.04f, 1f)
    val fraction by animateFloatAsState(targetValue = target, label = "trend_bar")

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(fraction)
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
            .background(
                if (value <= 0L) {
                    Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.22f)))
                } else {
                    Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f)))
                }
            )
    )
}

// -----------------------------------------------------------------------------
// Owe / owed
// -----------------------------------------------------------------------------

/**
 * The two figures that separate this app from a plain expense tracker: what the
 * user owes, and what is owed back to them. Kept side by side and never summed
 * into one number, because their direction is the information.
 */
@Composable
fun OweAndOwedRow(
    snapshot: DashboardSnapshot,
    onOpenDebts: () -> Unit,
    onOpenBorrowLend: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intrinsic height rather than two independently sized cards: the two
    // directions carry different numbers of breakdown rows, and a pair that
    // ends at different heights reads as one being more important.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DirectionCard(
            label = "You owe",
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            money = snapshot.totalOwed,
            currencySymbol = snapshot.currencySymbol,
            accent = MoneBieTheme.finance.negative,
            breakdown = listOf(
                "Debt" to snapshot.totalDebtOwed,
                "EMI" to snapshot.remainingEmiAmount,
                "Borrowed" to snapshot.totalMoneyBorrowed
            ),
            onClick = onOpenDebts,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        DirectionCard(
            label = "Owed to you",
            icon = Icons.Rounded.Savings,
            money = snapshot.totalOwedToUser,
            currencySymbol = snapshot.currencySymbol,
            accent = MoneBieTheme.finance.receivable,
            breakdown = listOf("Lent out" to snapshot.totalMoneyLent),
            onClick = onOpenBorrowLend,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun DirectionCard(
    label: String,
    icon: ImageVector,
    money: Money,
    currencySymbol: String,
    accent: Color,
    breakdown: List<Pair<String, Money>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier, contentPadding = 16.dp, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowBadge(icon = icon, tint = accent, size = 36.dp)
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        MicroLabel(text = label)

        Spacer(Modifier.height(4.dp))

        MoneyText(
            money = money,
            currencySymbol = currencySymbol,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            showDecimals = false
        )

        Spacer(Modifier.height(12.dp))

        CardDivider()

        Spacer(Modifier.height(8.dp))

        breakdown.forEach { (name, amount) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.55f))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = MoneyFormatter.compact(amount, currencySymbol),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Upcoming payments
// -----------------------------------------------------------------------------

/**
 * One due obligation.
 *
 * The left stripe carries urgency - overdue, due soon, receivable - so a row
 * can be triaged from the edge of the screen before any of it is read. Shared
 * with the reminders screen, which lists the same projection in full.
 */
@Composable
fun UpcomingPaymentRow(
    payment: UpcomingPayment,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val accent = when {
        payment.isOverdue -> MoneBieTheme.finance.negative
        payment.isReceivable -> MoneBieTheme.finance.receivable
        payment.daysUntilDue <= 7L -> MoneBieTheme.finance.caution
        else -> MaterialTheme.colorScheme.primary
    }
    val accentContainer = when {
        payment.isOverdue -> MoneBieTheme.finance.negativeContainer
        payment.isReceivable -> MoneBieTheme.finance.receivableContainer
        payment.daysUntilDue <= 7L -> MoneBieTheme.finance.cautionContainer
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    }

    PremiumCard(modifier = modifier.fillMaxWidth(), contentPadding = 0.dp) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Urgency stripe: the row's status, readable before the text is.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.45f)))
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DueDateBlock(dueDate = payment.dueDate, accent = accent, container = accentContainer)

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (payment.kind) {
                                UpcomingPaymentKind.DEBT -> Icons.AutoMirrored.Rounded.ReceiptLong
                                UpcomingPaymentKind.EMI -> Icons.Rounded.EventRepeat
                                UpcomingPaymentKind.BORROWED,
                                UpcomingPaymentKind.LENT -> Icons.Rounded.Handshake
                            },
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = payment.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = payment.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    StatusPill(
                        text = relativeDueLabel(payment.daysUntilDue),
                        contentColor = accent,
                        containerColor = accentContainer
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    MoneyText(
                        money = payment.amount,
                        currencySymbol = currencySymbol,
                        style = MaterialTheme.typography.titleSmall,
                        showDecimals = false
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatDisplayDate(payment.dueDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Calendar-leaf marker: the day of the month above its month abbreviation. */
@Composable
private fun DueDateBlock(
    dueDate: String,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .size(width = 46.dp, height = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayOfMonth(dueDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1
        )
        Text(
            text = monthAbbreviation(dueDate),
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}

private val MonthAbbreviations = listOf(
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
)

private val BsMonthAbbreviations = listOf(
    "BAI", "JES", "ASA", "SHR", "BHA", "ASH", "KAR", "MAN", "POU", "MAG", "FAL", "CHA"
)

/** The BS form of [day] when the user reads dates in BS, else null. */
private fun bsDateOrNull(day: String): BsDate? =
    if (AppCalendar.current == CalendarSystem.BS) NepaliCalendar.fromAd(day) else null

/**
 * Read straight off the stored `yyyy-MM-dd` rather than through the platform
 * date formatter, so the block never depends on how a locale orders a date.
 */
private fun dayOfMonth(day: String): String {
    bsDateOrNull(day)?.let { return it.day.toString() }
    return day.takeIf { it.length >= 10 }?.substring(8, 10)?.trimStart('0').orEmpty().ifEmpty { "-" }
}

private fun monthAbbreviation(day: String): String {
    bsDateOrNull(day)?.let { return BsMonthAbbreviations[it.month - 1] }
    val index = day.takeIf { it.length >= 7 }?.substring(5, 7)?.toIntOrNull() ?: return ""
    return MonthAbbreviations.getOrElse(index - 1) { "" }
}

/**
 * Something is already late.
 *
 * Deliberately carries no count or total: the dashboard only receives the next
 * few obligations, so a number taken from that list would understate a bad
 * month. It says what is true and hands over to the reminder screen, which
 * holds the whole set.
 */
@Composable
fun OverduePrompt(
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MoneBieTheme.finance.negative

    PremiumCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 14.dp,
        onClick = onOpenReminders
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowBadge(icon = Icons.Rounded.PriorityHigh, tint = accent, size = 38.dp)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Something is past its due date",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Open your upcoming payments to see what",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun UpcomingPaymentsEmpty(modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier.fillMaxWidth()) {
        EmptyState(
            icon = Icons.Rounded.EventRepeat,
            title = "Nothing due",
            message = "No debts, EMIs or personal loans have an upcoming due date."
        )
    }
}

// -----------------------------------------------------------------------------
// Recent transactions
// -----------------------------------------------------------------------------

@Composable
fun TransactionRow(
    transaction: Transaction,
    accountName: String,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    // Passed in rather than read off the transaction: the name and icon are
    // the user's to change, and the screen holding the loaded set is the only
    // thing that knows what they are today.
    category: TransactionCategory = transaction.category
) {
    // A transfer is neither a gain nor a loss, so it stays neutral rather than
    // borrowing the expense colour and reading as money leaving.
    val accent = when (transaction.type) {
        TransactionType.INCOME -> MoneBieTheme.finance.positive
        TransactionType.EXPENSE -> MoneBieTheme.finance.negative
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlowBadge(
            icon = MoneBieIcons.forCategory(category.iconKey),
            tint = accent
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.note?.takeIf { it.isNotBlank() } ?: category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = category.name + " · " + accountName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Marks a transaction the app created on the user's behalf, so it is
            // clear why tapping it does not open the editor: it is owned by the
            // record that produced it and is managed from there.
            if (transaction.source.isSystemGenerated) {
                Spacer(Modifier.height(5.dp))
                StatusPill(
                    text = when (transaction.source.type) {
                        TransactionSourceType.DEBT_PAYMENT -> "Linked to a debt payment"
                        TransactionSourceType.EMI_PAYMENT -> "Linked to an EMI installment"
                        // Principal movements and repayments alike belong to a
                        // borrow/lend record, and none of the four is spending.
                        TransactionSourceType.BORROW_RECEIVED,
                        TransactionSourceType.BORROW_REPAYMENT,
                        TransactionSourceType.LEND_GIVEN,
                        TransactionSourceType.LEND_REPAYMENT -> "Linked to a personal loan"
                        else -> "Recorded automatically"
                    },
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
            }
        }

        MoneyText(
            money = when (transaction.type) {
                TransactionType.EXPENSE -> -transaction.amount
                else -> transaction.amount
            },
            currencySymbol = currencySymbol,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            signed = transaction.type != TransactionType.TRANSFER,
            showDecimals = false
        )
    }
}

// -----------------------------------------------------------------------------
// Accounts strip
// -----------------------------------------------------------------------------

@Composable
fun AccountChip(
    account: Account,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    // A card at -20,000 is money owed, not money available, and it says so here
    // in the same colour the accounts screen uses for it.
    val negative = account.balance.isNegative
    val accent = if (negative) MoneBieTheme.finance.negative else MaterialTheme.colorScheme.primary

    PremiumCard(modifier = modifier.width(158.dp), contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowBadge(
                icon = MoneBieIcons.forAccount(account.type.iconKey),
                tint = accent,
                size = 34.dp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = account.type.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = account.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            MoneyText(
                money = if (negative) account.balance.absolute else account.balance,
                currencySymbol = currencySymbol,
                style = MaterialTheme.typography.titleSmall,
                color = if (negative) accent else MaterialTheme.colorScheme.onSurface,
                showDecimals = false
            )
            if (negative) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "owed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = 0.55f),
                            if (negative) {
                                accent.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
                            }
                        )
                    )
                )
        )
    }
}

/**
 * This month's budgets at a glance.
 *
 * Shows the three that most need attention - over first, then closest to the
 * line - because the dashboard's job is to point, not to list. The full set
 * is one tap away.
 */
@Composable
fun BudgetsCard(
    budgets: List<BudgetStatus>,
    currencySymbol: String,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val over = budgets.count { it.isOver }
    val near = budgets.count { it.isNearLimit }
    val totalLimit = Money(budgets.sumOf { it.limit.minorUnits })
    val totalSpent = Money(budgets.sumOf { it.spent.minorUnits })
    val remaining = totalLimit - totalSpent

    PremiumCard(modifier = modifier.fillMaxWidth(), onClick = onOpenBudgets) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(text = "Budgets this month")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        over > 0 -> "$over over the limit"
                        near > 0 -> "$near close to the limit"
                        else -> "All within budget"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        over > 0 -> MoneBieTheme.finance.negative
                        near > 0 -> MoneBieTheme.finance.caution
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (remaining.isNegative) "Over by" else "Left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MoneyText(
                    money = remaining.absolute,
                    currencySymbol = currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (remaining.isNegative) MoneBieTheme.finance.negative else MaterialTheme.colorScheme.onSurface,
                    showDecimals = false
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.medium))

        budgets.take(3).forEachIndexed { index, status ->
            if (index > 0) Spacer(Modifier.height(MoneBieTheme.spacing.small))
            BudgetMiniRow(status = status, currencySymbol = currencySymbol)
        }
    }
}

@Composable
private fun BudgetMiniRow(status: BudgetStatus, currencySymbol: String) {
    val accent = when (status.level) {
        BudgetLevel.OK -> MoneBieTheme.finance.positive
        BudgetLevel.NEAR -> MoneBieTheme.finance.caution
        BudgetLevel.OVER -> MoneBieTheme.finance.negative
    }
    val width by animateFloatAsState(targetValue = status.progress, label = "budget_mini")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.category.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = MoneyFormatter.compact(status.spent, currencySymbol) + " / " +
                    MoneyFormatter.compact(status.limit, currencySymbol),
                style = MaterialTheme.typography.labelSmall,
                color = if (status.isOver) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(width)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}
