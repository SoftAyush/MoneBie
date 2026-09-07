package com.aurex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurex.app.core.dayToEpochMillis
import com.aurex.app.core.epochMillisToDay
import com.aurex.app.core.formatDisplayDate
import com.aurex.app.core.formatWeekday
import com.aurex.app.core.getCurrentDay
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.displayName
import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.iconKey
import com.aurex.app.ui.theme.AurexTheme

/**
 * Tappable date row backed by the Material date picker.
 *
 * Shared by every form that takes a date, so "Today" is labelled the same way
 * and an optional date is cleared the same way everywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AurexDateField(
    day: String?,
    onDayChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select a date",
    /** Shows a clear button, for dates that are genuinely optional. */
    clearable: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }

    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(AurexTheme.spacing.medium)
    val isToday = day != null && day == getCurrentDay()
    val isSet = day != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outline.copy(alpha = 0.3f), shape)
            .pressable { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlowBadge(
            icon = Icons.Rounded.CalendarMonth,
            tint = if (isSet) scheme.primary else scheme.onSurfaceVariant,
            size = 36.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (day == null) placeholder else formatDisplayDate(day),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSet) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSet) {
                    scheme.onSurface
                } else {
                    scheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (day != null) {
                Spacer(Modifier.height(1.dp))
                // The weekday earns its line: a date on a form is usually being
                // checked against a real day, not just read back.
                MicroLabel(
                    text = if (isToday) "Today" else formatWeekday(day),
                    color = if (isToday) scheme.primary else scheme.onSurfaceVariant
                )
            }
        }

        if (clearable && day != null) {
            Spacer(Modifier.width(AurexTheme.spacing.small))
            GlassIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Clear date",
                onClick = { onDayChange(null) },
                tint = scheme.onSurfaceVariant,
                size = 30.dp
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dayToEpochMillis(day ?: getCurrentDay())
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            shape = RoundedCornerShape(28.dp),
            colors = aurexDatePickerColors(),
            confirmButton = {
                // The primary action wears the brand fill: on a calendar full of
                // tappable numbers, two identical text buttons in the corner are
                // the easiest thing on the dialog to miss.
                Text(
                    text = "Select",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = onBrandColor(),
                    modifier = Modifier
                        .padding(end = AurexTheme.spacing.small, bottom = 4.dp)
                        .clip(CircleShape)
                        .background(brandBrush())
                        .pressable {
                            pickerState.selectedDateMillis?.let {
                                onDayChange(epochMillisToDay(it))
                            }
                            showPicker = false
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            },
            dismissButton = {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(CircleShape)
                        .pressable { showPicker = false }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = aurexDatePickerColors(),
                title = {
                    Column(
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 20.dp
                        )
                    ) {
                        MicroLabel(text = "Pick a date", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}

/**
 * The calendar in the app's own colours.
 *
 * Material's defaults put the selection on the tonal primary container, which
 * on this palette is a pale blue that reads as "disabled" beside the brand fill
 * used for every other selected thing in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun aurexDatePickerColors(): DatePickerColors {
    val scheme = MaterialTheme.colorScheme

    return DatePickerDefaults.colors(
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurfaceVariant,
        headlineContentColor = scheme.onSurface,
        weekdayContentColor = scheme.onSurfaceVariant.copy(alpha = 0.7f),
        subheadContentColor = scheme.primary,
        dayContentColor = scheme.onSurface,
        selectedDayContentColor = scheme.onPrimary,
        selectedDayContainerColor = scheme.primary,
        // Today is outlined rather than filled, so it never competes with the
        // day actually selected.
        todayContentColor = scheme.primary,
        todayDateBorderColor = scheme.primary,
        dividerColor = scheme.outline.copy(alpha = 0.2f)
    )
}

/**
 * The accounts to pick from, with what is in each.
 *
 * Balances are on show because the choice is usually "which account can cover
 * this", and making the user leave the form to check defeats fast entry.
 */
@Composable
fun AccountPicker(
    accounts: List<Account>,
    selectedId: Long?,
    currencySymbol: String,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No account available"
) {
    if (accounts.isEmpty()) {
        FormNotice(
            icon = Icons.Rounded.AccountBalanceWallet,
            text = emptyMessage,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        accounts.forEach { account ->
            AccountOption(
                account = account,
                isSelected = account.id == selectedId,
                currencySymbol = currencySymbol,
                onClick = { onSelect(account.id) }
            )
        }
    }
}

/**
 * One account to choose.
 *
 * A negative balance takes the negative colour and reads as owed, the same way
 * it does on the accounts screen: a card at -20,000 is not twenty thousand
 * available to spend, and a picker is exactly where that would be misread.
 */
@Composable
private fun AccountOption(
    account: Account,
    isSelected: Boolean,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val negative = account.balance.isNegative
    val accent = if (negative) AurexTheme.finance.negative else scheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(scheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, scheme.primary.copy(alpha = 0.45f), shape)
                } else {
                    Modifier
                        .background(scheme.surfaceContainer.copy(alpha = 0.7f))
                        .border(1.dp, scheme.outline.copy(alpha = 0.14f), shape)
                }
            )
            .pressable(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlowBadge(
            icon = AurexIcons.forAccount(account.type.iconKey),
            tint = accent,
            size = 34.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) scheme.primary else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = account.type.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(Modifier.width(AurexTheme.spacing.small))

        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                money = if (negative) account.balance.absolute else account.balance,
                currencySymbol = currencySymbol,
                style = MaterialTheme.typography.titleSmall,
                color = if (negative) AurexTheme.finance.negative else scheme.onSurface,
                showDecimals = false
            )
            Spacer(Modifier.height(1.dp))
            MicroLabel(text = if (negative) "owed" else "available")
        }

        if (isSelected) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(brandBrush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = onBrandColor(),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Category
// -----------------------------------------------------------------------------

/**
 * The category, collapsed to one line.
 *
 * Every category used to be laid out at once, which put fourteen pills between
 * the amount and the account and pushed the rest of the form off the screen.
 * The list is worth seeing while you choose and worth nothing once you have, so
 * it opens on demand and closes behind the choice.
 *
 * The current category stays legible while collapsed - its icon and its name,
 * not just a name - because that is the whole of what the field is for once the
 * sheet has closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    categories: List<TransactionCategory>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    var showSheet by remember { mutableStateOf(false) }

    // Falls back to the first rather than showing an empty field: a form that
    // has to file the entry somewhere should say where it currently would.
    val selected = categories.firstOrNull { it.id == selectedId } ?: categories.firstOrNull()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainer.copy(alpha = 0.7f))
            .border(1.dp, scheme.outline.copy(alpha = 0.20f), shape)
            .pressable { if (categories.isNotEmpty()) showSheet = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlowBadge(
            icon = AurexIcons.forCategory(selected?.iconKey.orEmpty()),
            tint = scheme.primary,
            size = 34.dp
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = selected?.name ?: "No categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected != null) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Choose a category",
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = scheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            CategorySheet(
                categories = categories,
                selectedId = selected?.id,
                onPick = { id ->
                    onSelect(id)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
private fun CategorySheet(
    categories: List<TransactionCategory>,
    selectedId: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AurexTheme.spacing.medium)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowBadge(icon = Icons.Rounded.Sell, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(AurexTheme.spacing.medium))
            Column {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(
                    text = if (categories.size == 1) {
                        "1 to choose from"
                    } else {
                        "${categories.size} to choose from"
                    }
                )
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        // Scrolls rather than fits: a user who has added their own categories
        // can have far more than a screenful, and the sheet must not cut them.
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryOption(
                    category = category,
                    isSelected = category.id == selectedId,
                    onClick = { onPick(category.id) }
                )
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
    }
}

@Composable
private fun CategoryOption(
    category: TransactionCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(scheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, scheme.primary.copy(alpha = 0.45f), shape)
                } else {
                    Modifier
                        .background(scheme.surfaceContainer.copy(alpha = 0.7f))
                        .border(1.dp, scheme.outline.copy(alpha = 0.14f), shape)
                }
            )
            .pressable(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlowBadge(
            icon = AurexIcons.forCategory(category.iconKey),
            tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
            size = 34.dp
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) scheme.primary else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(brandBrush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = onBrandColor(),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
