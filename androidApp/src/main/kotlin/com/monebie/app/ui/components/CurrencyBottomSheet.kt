package com.monebie.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.monebie.app.domain.model.Currency
import com.monebie.app.ui.theme.MoneBieTheme
import com.monebie.app.ui.theme.Palette700
import com.monebie.app.ui.theme.Palette900

/**
 * Picking the currency every figure in the app is labelled with.
 *
 * A hundred and fifty rows is too many to browse, so the sheet is built around
 * finding rather than scrolling. What is set now leads as a card of its own -
 * the one thing a user opens this to check - then the handful most people want,
 * then search, then everything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyBottomSheet(
    currencies: List<Currency>,
    selected: Currency,
    onDismiss: () -> Unit,
    onSelect: (Currency) -> Unit
) {
    var search by remember { mutableStateOf("") }

    val filtered = remember(search, currencies) {
        val query = search.trim()
        if (query.isBlank()) {
            currencies
        } else {
            currencies.filter {
                it.code.contains(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.symbol.contains(query, ignoreCase = true)
            }
        }
    }

    val searching = search.isNotBlank()
    val popular = remember(currencies) { currencies.filter { it.popular } }
    val listed = if (searching) filtered else currencies

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.92f)) {

            SelectedCurrencyCard(
                currency = selected,
                modifier = Modifier.padding(horizontal = MoneBieTheme.spacing.medium)
            )

            Spacer(Modifier.height(MoneBieTheme.spacing.large))

            MoneBieTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search by name, code or symbol",
                leadingIcon = Icons.Rounded.Search,
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = MoneBieTheme.spacing.medium)
            )

            // The quick row is a shortcut, not a filter result - it would be
            // noise beside a search that already narrowed the list.
            if (!searching && popular.isNotEmpty()) {
                Spacer(Modifier.height(MoneBieTheme.spacing.large))

                MicroLabel(
                    text = "Most used",
                    modifier = Modifier.padding(horizontal = MoneBieTheme.spacing.medium)
                )

                Spacer(Modifier.height(MoneBieTheme.spacing.small))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = MoneBieTheme.spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
                ) {
                    items(popular, key = { "quick_" + it.code }) { currency ->
                        QuickCurrencyCard(
                            currency = currency,
                            isSelected = currency.code == selected.code,
                            onClick = { onSelect(currency) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(MoneBieTheme.spacing.large))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MoneBieTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MicroLabel(text = if (searching) "Results" else "All currencies")
                Spacer(Modifier.weight(1f))
                MicroLabel(
                    text = if (listed.size == 1) "1 currency" else "${listed.size} currencies",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(MoneBieTheme.spacing.small))

            if (listed.isEmpty()) {
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MoneBieTheme.spacing.medium)
                ) {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = "No match",
                        message = "Nothing here is called \"" + search.trim() + "\". Try the " +
                            "three-letter code, like USD."
                    )
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = MoneBieTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(listed, key = { it.code }) { currency ->
                    CurrencyRow(
                        currency = currency,
                        isSelected = currency.code == selected.code,
                        onClick = { onSelect(currency) }
                    )
                }

                item(key = "bottom_inset") {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

/**
 * The currency in force, as a card rather than a line of text.
 *
 * It is the one thing a user opens this sheet to check, and the symbol shown at
 * size is the actual answer to "what will my balances look like" - a three
 * letter code beside a flag is not.
 */
@Composable
private fun SelectedCurrencyCard(
    currency: Currency,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = HeroCardShape,
                ambientColor = Palette900,
                spotColor = Palette700
            )
            .clip(HeroCardShape)
            .background(heroBrush())
    ) {
        HeroDecoration(Modifier.matchParentSize())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = currency.flag, style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(text = "Showing amounts in", color = OnHero.copy(alpha = 0.75f))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnHero,
                    maxLines = 1
                )
                Text(
                    text = currency.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnHero.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            // The sample the whole choice is really about: what a figure will
            // be prefixed with everywhere in the app.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = currency.symbol,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnHero,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * A one-tap card for a currency most people want.
 *
 * Wide enough for the longest symbol in the popular set and no wider, so the
 * row scans as a row of equals rather than of odd sizes.
 */
@Composable
private fun QuickCurrencyCard(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    val lift by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.97f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "quick_card_lift"
    )

    Column(
        modifier = modifier
            .width(84.dp)
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
            }
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(brandBrush())
                } else {
                    Modifier
                        .background(scheme.surfaceContainer.copy(alpha = 0.7f))
                        .border(1.dp, scheme.outline.copy(alpha = 0.16f), shape)
                }
            )
            .pressable(onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = currency.flag, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(6.dp))

        Text(
            text = currency.code,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) onBrandColor() else scheme.onSurface,
            maxLines = 1
        )

        Text(
            text = currency.symbol,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                onBrandColor().copy(alpha = 0.8f)
            } else {
                scheme.onSurfaceVariant
            },
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * One currency in the full list.
 *
 * The selected row is filled and ringed in the brand colour rather than only
 * carrying a tick: in a list this long, a small mark at the end of a row is
 * easy to scroll straight past.
 */
@Composable
private fun CurrencyRow(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

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
                    Modifier.background(scheme.surfaceContainer.copy(alpha = 0.55f))
                }
            )
            .pressable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        scheme.primary.copy(alpha = 0.16f)
                    } else {
                        scheme.surfaceContainerHighest
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) {
                        scheme.primary.copy(alpha = 0.35f)
                    } else {
                        scheme.outline.copy(alpha = 0.18f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = currency.flag, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) scheme.primary else scheme.onSurface,
                maxLines = 1
            )
            Text(
                text = currency.name,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(MoneBieTheme.spacing.small))

        // Reserve the column and forbid wrapping: symbols like the Moroccan
        // dirham (د.م.) are several characters, and left to itself the trailing
        // mark breaks onto a second line.
        Text(
            text = currency.symbol,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 44.dp)
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
