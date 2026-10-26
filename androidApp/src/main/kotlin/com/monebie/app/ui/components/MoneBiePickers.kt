package com.monebie.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.monebie.app.core.BsDate
import com.monebie.app.core.CalendarSystem
import com.monebie.app.core.NepaliCalendar
import com.monebie.app.core.dayToEpochMillis
import com.monebie.app.core.epochMillisToDay
import com.monebie.app.core.formatDisplayDate
import com.monebie.app.core.formatGregorianDate
import com.monebie.app.core.formatNepaliDate
import com.monebie.app.core.formatWeekday
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.displayName
import com.monebie.app.domain.model.TransactionCategory
import com.monebie.app.domain.model.iconKey
import com.monebie.app.ui.theme.MoneBieTheme

/**
 * Tappable date row backed by the Material date picker, or a Bikram Sambat
 * month grid when the user reads dates in BS (chosen in Settings).
 *
 * Shared by every form that takes a date, so "Today" is labelled the same way
 * and an optional date is cleared the same way everywhere. The dialog opens in
 * the app's calendar and can peek at the other one without changing the
 * setting. The value handed back is always an AD day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneBieDateField(
    day: String?,
    onDayChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select a date",
    /** Shows a clear button, for dates that are genuinely optional. */
    clearable: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }

    val calendar = currentCalendar()
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(MoneBieTheme.spacing.medium)
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
                // checked against a real day, not just read back. The other
                // calendar rides along because bills and slips use either.
                val other = if (calendar == CalendarSystem.BS) {
                    formatGregorianDate(day)
                } else {
                    formatNepaliDate(day)
                }
                val weekday = if (isToday) "Today" else formatWeekday(day)
                MicroLabel(
                    text = if (other != null) "$weekday · $other" else weekday,
                    color = if (isToday) scheme.primary else scheme.onSurfaceVariant
                )
            }
        }

        if (clearable && day != null) {
            Spacer(Modifier.width(MoneBieTheme.spacing.small))
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
        val initialDay = day ?: getCurrentDay()
        var mode by remember { mutableStateOf(calendar) }
        // The BS grid's selection. The AD picker keeps its own, and the two are
        // synced whenever the user switches calendars.
        var bsSelected by remember { mutableStateOf(initialDay) }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dayToEpochMillis(initialDay)
        )

        val switchTo: (CalendarSystem) -> Unit = { target ->
            if (target != mode) {
                if (target == CalendarSystem.BS) {
                    pickerState.selectedDateMillis?.let { bsSelected = epochMillisToDay(it) }
                } else {
                    val millis = dayToEpochMillis(bsSelected)
                    pickerState.selectedDateMillis = millis
                    pickerState.displayedMonthMillis = millis
                }
                mode = target
            }
        }

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            shape = RoundedCornerShape(28.dp),
            colors = monebieDatePickerColors(),
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
                        .padding(end = MoneBieTheme.spacing.small, bottom = 4.dp)
                        .clip(CircleShape)
                        .background(brandBrush())
                        .pressable {
                            val picked = if (mode == CalendarSystem.AD) {
                                pickerState.selectedDateMillis?.let(::epochMillisToDay)
                            } else {
                                bsSelected
                            }
                            picked?.let(onDayChange)
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
            val header: @Composable () -> Unit = {
                CalendarToggle(
                    selected = mode,
                    onSelect = switchTo,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)
                )
            }
            if (mode == CalendarSystem.AD) {
                DatePicker(
                    state = pickerState,
                    colors = monebieDatePickerColors(),
                    title = header
                )
            } else {
                NepaliDatePicker(
                    selectedDay = bsSelected,
                    onSelect = { bsSelected = it },
                    header = header
                )
            }
        }
    }
}

/** The AD / BS switch at the top of the date dialog. */
@Composable
private fun CalendarToggle(
    selected: CalendarSystem,
    onSelect: (CalendarSystem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
    ) {
        SelectableChip(
            label = "English (AD)",
            selected = selected == CalendarSystem.AD,
            onClick = { onSelect(CalendarSystem.AD) },
            modifier = Modifier.weight(1f)
        )
        SelectableChip(
            label = "नेपाली (BS)",
            selected = selected == CalendarSystem.BS,
            onClick = { onSelect(CalendarSystem.BS) },
            modifier = Modifier.weight(1f)
        )
    }
}

private val bsWeekdays = listOf("आइत", "सोम", "मंगल", "बुध", "बिही", "शुक्र", "शनि")

/**
 * A month of the Bikram Sambat calendar, laid out like the Material picker it
 * stands in for so switching calendars does not feel like opening another app.
 *
 * Always six rows tall, so the dialog does not jump in height between months.
 */
@Composable
private fun NepaliDatePicker(
    selectedDay: String,
    onSelect: (String) -> Unit,
    header: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val today = getCurrentDay()
    val selectedBs = NepaliCalendar.fromAd(selectedDay)
    val todayBs = NepaliCalendar.fromAd(today)
    val start = selectedBs ?: todayBs ?: BsDate(NepaliCalendar.MAX_YEAR, 12, 1)

    var year by remember { mutableStateOf(start.year) }
    var month by remember { mutableStateOf(start.month) }

    fun shiftMonths(delta: Int) {
        val index = (year * 12 + month - 1 + delta)
            .coerceIn(NepaliCalendar.MIN_YEAR * 12, NepaliCalendar.MAX_YEAR * 12 + 11)
        year = index / 12
        month = index % 12 + 1
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        header()

        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                text = formatNepaliDate(selectedDay) ?: formatGregorianDate(selectedDay),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            MicroLabel(text = "${formatWeekday(selectedDay)} · ${formatGregorianDate(selectedDay)}")
        }

        HorizontalDivider(color = scheme.outline.copy(alpha = 0.2f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val atStart = year == NepaliCalendar.MIN_YEAR && month == 1
            val atEnd = year == NepaliCalendar.MAX_YEAR && month == 12
            IconButton(onClick = { shiftMonths(-12) }, enabled = !atStart) {
                Icon(Icons.Rounded.KeyboardDoubleArrowLeft, contentDescription = "Previous year")
            }
            IconButton(onClick = { shiftMonths(-1) }, enabled = !atStart) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${NepaliCalendar.monthNames[month - 1]} $year",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Text(
                    text = NepaliCalendar.monthNamesNepali[month - 1],
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { shiftMonths(1) }, enabled = !atEnd) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
            }
            IconButton(onClick = { shiftMonths(12) }, enabled = !atEnd) {
                Icon(Icons.Rounded.KeyboardDoubleArrowRight, contentDescription = "Next year")
            }
        }

        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                bsWeekdays.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        // Saturday is the weekly holiday, so it gets the accent.
                        color = if (index == 6) scheme.error else scheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp)
                    )
                }
            }

            val leading = NepaliCalendar.weekday(BsDate(year, month, 1))
            val length = NepaliCalendar.daysInMonth(year, month)
            for (week in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (column in 0 until 7) {
                        val date = week * 7 + column - leading + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date in 1..length) {
                                BsDayCell(
                                    date = date,
                                    selected = BsDate(year, month, date) == selectedBs,
                                    today = BsDate(year, month, date) == todayBs,
                                    holiday = column == 6,
                                    onClick = { onSelect(NepaliCalendar.toAd(BsDate(year, month, date))) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One day in the BS grid, styled to match the Material picker's days. */
@Composable
private fun BsDayCell(
    date: Int,
    selected: Boolean,
    today: Boolean,
    holiday: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val outlined = today && !selected

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) scheme.primary else Color.Transparent)
            .border(
                width = if (outlined) 1.dp else 0.dp,
                color = if (outlined) scheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                selected -> scheme.onPrimary
                today -> scheme.primary
                holiday -> scheme.error
                else -> scheme.onSurface
            }
        )
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
private fun monebieDatePickerColors(): DatePickerColors {
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
        verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
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
    val accent = if (negative) MoneBieTheme.finance.negative else scheme.primary

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
            icon = MoneBieIcons.forAccount(account.type.iconKey),
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

        Spacer(Modifier.width(MoneBieTheme.spacing.small))

        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                money = if (negative) account.balance.absolute else account.balance,
                currencySymbol = currencySymbol,
                style = MaterialTheme.typography.titleSmall,
                color = if (negative) MoneBieTheme.finance.negative else scheme.onSurface,
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
            icon = MoneBieIcons.forCategory(selected?.iconKey.orEmpty()),
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
            .padding(horizontal = MoneBieTheme.spacing.medium)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowBadge(icon = Icons.Rounded.Sell, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(MoneBieTheme.spacing.medium))
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

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        // Scrolls rather than fits: a user who has added their own categories
        // can have far more than a screenful, and the sheet must not cut them.
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryOption(
                    category = category,
                    isSelected = category.id == selectedId,
                    onClick = { onPick(category.id) }
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.extraExtraLarge))
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
            icon = MoneBieIcons.forCategory(category.iconKey),
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
