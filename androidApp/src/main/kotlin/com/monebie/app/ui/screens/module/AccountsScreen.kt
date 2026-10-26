package com.monebie.app.ui.screens.module

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.core.Money
import com.monebie.app.core.MoneyFormatter
import com.monebie.app.core.total
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.model.displayName
import com.monebie.app.domain.model.iconKey
import com.monebie.app.domain.model.isAsset
import com.monebie.app.presentation.account.AccountEvent
import com.monebie.app.presentation.account.AccountFormState
import com.monebie.app.presentation.account.AccountListUiState
import com.monebie.app.presentation.account.AccountListViewModel
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.MoneBieButton
import com.monebie.app.ui.components.MoneBieSnackbarHost
import com.monebie.app.ui.components.showMoneBieMessage
import com.monebie.app.ui.components.MoneBieIcons
import com.monebie.app.ui.components.MoneBieTextField
import com.monebie.app.ui.components.ConfirmDialog
import com.monebie.app.ui.components.CountUpMoneyText
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.EmptyState
import com.monebie.app.ui.components.FilterPill
import com.monebie.app.ui.components.FormField
import com.monebie.app.ui.components.FormNotice
import com.monebie.app.ui.components.GlassIconButton
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.GradientTrack
import com.monebie.app.ui.components.HeroCardShape
import com.monebie.app.ui.components.HeroDecoration
import com.monebie.app.ui.components.HeroFinance
import com.monebie.app.ui.components.HeroIconButton
import com.monebie.app.ui.components.HeroTile
import com.monebie.app.ui.components.MicroLabel
import com.monebie.app.ui.components.MoneyInputField
import com.monebie.app.ui.components.MoneyText
import com.monebie.app.ui.components.OnHero
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.PremiumSectionHeader
import com.monebie.app.ui.components.ShimmerBlock
import com.monebie.app.ui.components.heroBrush
import com.monebie.app.ui.components.pressable
import com.monebie.app.ui.components.maskedValue
import com.monebie.app.ui.theme.MoneBieTheme
import com.monebie.app.ui.theme.Palette300
import com.monebie.app.ui.theme.Palette500
import com.monebie.app.ui.theme.Palette700
import com.monebie.app.ui.theme.Palette900

/**
 * Where the money actually sits.
 *
 * The accounts holding money and the accounts carrying a balance owed are
 * listed apart: a credit card at -20,000 and a savings account at 20,000 are
 * opposite facts, and a single list would let the eye add them together.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun AccountsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: AccountListViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AccountListViewModel(
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    getCurrentUserUseCase = app.appContainer.getCurrentUserUseCase,
                    saveAccountUseCase = app.appContainer.saveAccountUseCase,
                    archiveAccountUseCase = app.appContainer.archiveAccountUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingArchive by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(event) {
        when (val current = event) {
            is AccountEvent.Saved -> snackbarHostState.showMoneBieMessage("Saved ${current.name}.")
            is AccountEvent.Archived -> snackbarHostState.showMoneBieMessage(
                "${current.name} is archived. Its transactions keep their history."
            )

            is AccountEvent.LastAccountRefused -> snackbarHostState.showMoneBieMessage(
                "Keep at least one account - everything you record has to move money somewhere."
            )

            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    val holding = uiState.accounts.filter { it.isAssetAccount }
    val owing = uiState.accounts.filterNot { it.isAssetAccount }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Accounts",
                onBack = onBack,
                subtitle = when {
                    uiState.isLoading -> "Loading"
                    uiState.accounts.size == 1 -> "1 account"
                    else -> "${uiState.accounts.size} accounts"
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = MoneBieTheme.spacing.medium,
                    end = MoneBieTheme.spacing.medium,
                    top = MoneBieTheme.spacing.small,
                    bottom = MoneBieTheme.spacing.extraExtraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
            ) {
                if (uiState.isLoading) {
                    item(key = "skeleton") { AccountsSkeleton() }
                } else {
                    item(key = "hero") {
                        BalanceHero(
                            uiState = uiState,
                            holding = holding,
                            owing = owing
                        )
                    }
                }

                if (!uiState.isLoading && uiState.accounts.isEmpty()) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                title = "No accounts yet",
                                message = "Add a bank account, wallet or cash balance to start " +
                                    "tracking where your money sits.",
                                actionLabel = "Add an account",
                                onAction = viewModel::startCreating
                            )
                        }
                    }
                }

                if (holding.isNotEmpty()) {
                    item(key = "holding_header") {
                        // The subtotal belongs on the header rather than at the
                        // foot of the list: it is what the section adds up to,
                        // and it is read before the rows, not after them.
                        PremiumSectionHeader(
                            title = "Holding money",
                            trailing = {
                                SectionTotal(
                                    money = holding.map { it.balance }.total(),
                                    currencySymbol = uiState.currencySymbol
                                )
                            }
                        )
                    }
                    items(holding, key = { "asset_" + it.id }) { account ->
                        AccountCard(
                            account = account,
                            currencySymbol = uiState.currencySymbol,
                            share = account.balance.shareOf(holding),
                            canArchive = uiState.canArchive,
                            onEdit = { viewModel.startEditing(account) },
                            onArchive = { pendingArchive = account }
                        )
                    }
                }

                if (owing.isNotEmpty()) {
                    item(key = "owing_header") {
                        PremiumSectionHeader(
                            title = "Carrying a balance",
                            trailing = {
                                SectionTotal(
                                    money = owing.map { it.outstanding }.total(),
                                    currencySymbol = uiState.currencySymbol,
                                    color = MoneBieTheme.finance.negative
                                )
                            }
                        )
                    }
                    items(owing, key = { "liability_" + it.id }) { account ->
                        AccountCard(
                            account = account,
                            currencySymbol = uiState.currencySymbol,
                            share = null,
                            canArchive = uiState.canArchive,
                            onEdit = { viewModel.startEditing(account) },
                            onArchive = { pendingArchive = account }
                        )
                    }
                }

                if (!uiState.isLoading && uiState.accounts.isNotEmpty()) {
                    item(key = "add") {
                        AddAccountTile(
                            onClick = viewModel::startCreating,
                            modifier = Modifier.padding(top = MoneBieTheme.spacing.extraSmall)
                        )
                    }
                }
            }
        }

        /*
         * No floating button here, unlike the ledger.
         *
         * Adding an account sits at the end of the list instead. A person keeps
         * a handful of accounts, not a year of them, so the end of the list is
         * always within reach - and a floating button over a list this short
         * lands on top of the very tile offering the same action.
         */

        MoneBieSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    if (form.isVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissForm,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            AccountFormSheet(
                form = form,
                currencySymbol = uiState.currencySymbol,
                onNameChange = viewModel::setName,
                onTypeChange = viewModel::setType,
                onBalanceChange = viewModel::setOpeningBalance,
                onSave = viewModel::save
            )
        }
    }

    pendingArchive?.let { account ->
        ConfirmDialog(
            title = "Archive ${account.name}?",
            // Archived rather than deleted, and the copy says so: the word
            // "delete" beside a balance would suggest the history goes too.
            message = "It stops appearing when you record something, and its balance leaves " +
                "your totals. Everything already recorded against it stays exactly as it is.",
            confirmLabel = "Archive",
            isDestructive = true,
            onConfirm = {
                viewModel.archive(account)
                pendingArchive = null
            },
            onDismiss = { pendingArchive = null }
        )
    }
}

// -----------------------------------------------------------------------------
// The sheet
// -----------------------------------------------------------------------------

/**
 * Add or rename an account.
 *
 * The opening balance appears only when creating. An existing balance is the
 * sum of everything posted against the account, so a field here would let the
 * user type a figure that disagrees with its own transactions - the way to
 * change it is to record the movement that actually happened.
 */
@Composable
private fun AccountFormSheet(
    form: AccountFormState,
    currencySymbol: String,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onBalanceChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MoneBieTheme.spacing.medium)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowBadge(
                icon = MoneBieIcons.forAccount(form.type.iconKey),
                tint = if (form.type.isAsset) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MoneBieTheme.finance.negative
                }
            )
            Spacer(Modifier.width(MoneBieTheme.spacing.medium))
            Column {
                Text(
                    text = if (form.isEditing) "Edit account" else "New account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = form.name.trim().ifEmpty { form.type.displayName })
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        FormField(label = "Name", error = form.nameError) {
            MoneBieTextField(
                value = form.name,
                onValueChange = onNameChange,
                placeholder = "e.g. Everyday wallet",
                leadingIcon = Icons.Rounded.AccountBalanceWallet,
                textStyle = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        FormField(
            label = "Type",
            hint = if (form.type.isAsset) {
                null
            } else {
                "A card's balance counts against you, so it sits under what you owe."
            }
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
            ) {
                // Loans and EMIs have modules of their own, so they are not
                // offered here - tracking one as an account too would count the
                // same obligation twice on the balance sheet.
                listOf(
                    AccountType.BANK,
                    AccountType.DIGITAL_WALLET,
                    AccountType.CASH,
                    AccountType.CREDIT_CARD
                ).forEach { type ->
                    FilterPill(
                        label = type.displayName,
                        icon = MoneBieIcons.forAccount(type.iconKey),
                        selected = type == form.type,
                        onClick = { onTypeChange(type) }
                    )
                }
            }
        }

        if (!form.isEditing) {
            Spacer(Modifier.height(MoneBieTheme.spacing.large))

            FormField(
                label = if (form.type.isAsset) "Opening balance" else "Owed right now",
                error = form.balanceError,
                // A card and a bank account are opposite questions, and one
                // sentence cannot ask both: "what is in it" invites a figure
                // that means the reverse of what the user intends.
                hint = if (form.type.isAsset) {
                    "What is in it right now. Leave it empty to start from zero."
                } else {
                    "What you owe on it right now. Leave it empty to start from zero."
                }
            ) {
                MoneyInputField(
                    value = form.openingBalanceInput,
                    currencySymbol = currencySymbol,
                    onValueChange = onBalanceChange
                )
            }
        } else {
            Spacer(Modifier.height(MoneBieTheme.spacing.large))

            FormNotice(
                icon = Icons.Rounded.Info,
                text = "The balance comes from everything recorded against this account. " +
                    "Record a transaction to change it."
            )
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.extraLarge))

        MoneBieButton(
            text = if (form.isEditing) "Save changes" else "Add account",
            loading = form.isSaving,
            onClick = onSave
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.extraExtraLarge))
    }
}

/** This account's slice of everything held, or null when nothing is held at all. */
private fun Money.shareOf(accounts: List<Account>): Float? {
    val total = accounts.map { it.balance }.total()
    if (total.minorUnits <= 0L || minorUnits <= 0L) return null
    return (minorUnits.toDouble() / total.minorUnits.toDouble()).toFloat().coerceIn(0f, 1f)
}

// -----------------------------------------------------------------------------
// The hero
// -----------------------------------------------------------------------------

/**
 * Everything, and how it is spread.
 *
 * The strip under the figure is the point of this screen: a total says how much
 * there is, and only the split says whether it is sitting in one place.
 */
@Composable
private fun BalanceHero(
    uiState: AccountListUiState,
    holding: List<Account>,
    owing: List<Account>,
    modifier: Modifier = Modifier
) {
    var masked by rememberSaveable { mutableStateOf(false) }

    val held = holding.map { it.balance }.total()
    val owed = owing.map { it.outstanding }.total()

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
    ) {
        HeroDecoration(Modifier.matchParentSize())

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicroLabel(text = "Total balance", color = OnHero.copy(alpha = 0.75f))
                Spacer(Modifier.weight(1f))
                HeroIconButton(
                    icon = if (masked) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (masked) "Show balances" else "Hide balances",
                    onClick = { masked = !masked }
                )
            }

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = uiState.totalBalance,
                currencySymbol = uiState.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = OnHero,
                masked = masked
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = when {
                    uiState.accounts.isEmpty() -> "No accounts yet"
                    uiState.accounts.size == 1 -> "In one account"
                    else -> "Across ${uiState.accounts.size} accounts"
                },
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.75f)
            )

            if (holding.size > 1) {
                Spacer(Modifier.height(18.dp))

                CompositionStrip(accounts = holding)

                Spacer(Modifier.height(10.dp))

                CompositionLegend(accounts = holding)
            }

            /*
             * The split only when there is a split to show.
             *
             * With nothing owed, "held" is the figure already printed above it
             * and "owed 0" is a tile spent saying nothing - and both would
             * repeat the section subtotal a few rows further down.
             */
            if (owing.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroTile(
                        label = "Held",
                        value = if (masked) {
                            maskedValue()
                        } else {
                            MoneyFormatter.compact(held, uiState.currencySymbol)
                        },
                        valueColor = OnHero,
                        modifier = Modifier.weight(1f)
                    )
                    HeroTile(
                        label = "Owed on cards & loans",
                        value = if (masked) {
                            maskedValue()
                        } else {
                            MoneyFormatter.compact(owed, uiState.currencySymbol)
                        },
                        valueColor = if (owed.isZero) OnHero else HeroFinance.negative,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Every account holding money, as one bar.
 *
 * Segments run in the order the list below runs, so the widest band and the
 * top row are the same account without a label having to say so.
 */
@Composable
private fun CompositionStrip(
    accounts: List<Account>,
    modifier: Modifier = Modifier
) {
    val total = accounts.map { it.balance }.total().minorUnits
    val shares = accounts.map { account ->
        if (total <= 0L) 0f else (account.balance.minorUnits.toDouble() / total).toFloat()
    }
    val colors = accounts.indices.map { compositionColor(it) }

    val reveal by animateFloatAsState(
        targetValue = if (total > 0L) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "composition"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val radius = CornerRadius(size.height / 2f)
        val gap = 3.dp.toPx()

        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            size = size,
            cornerRadius = radius
        )

        var x = 0f
        shares.forEachIndexed { index, share ->
            // An account holding nothing gets no band rather than a sliver of
            // colour that suggests it holds a little.
            val width = (size.width * share.coerceAtLeast(0f) * reveal) - gap
            if (width > 0f) {
                drawRoundRect(
                    color = colors[index],
                    topLeft = Offset(x, 0f),
                    size = Size(width, size.height),
                    cornerRadius = radius
                )
            }
            x += size.width * share.coerceAtLeast(0f) * reveal
        }
    }
}

/**
 * Which band is which.
 *
 * Every account holding anything gets an entry, however little. The threshold
 * this used to apply meant the small account was the one you could neither see
 * in the bar nor find in the legend - which is exactly the account a split is
 * supposed to reveal. Entries wrap rather than compete for one line, and a
 * long tail collapses into a count.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompositionLegend(
    accounts: List<Account>,
    modifier: Modifier = Modifier
) {
    val total = accounts.map { it.balance }.total().minorUnits
    if (total <= 0L) return

    val named = accounts.take(3)
    val remaining = accounts.drop(3).count { it.balance.minorUnits > 0L }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        named.forEachIndexed { index, account ->
            val share = (account.balance.minorUnits.toDouble() / total).toFloat()
            if (share > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(compositionColor(index))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = account.name + " " + MoneyFormatter.percent(share),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnHero.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (remaining > 0) {
            Text(
                text = "+$remaining more",
                style = MaterialTheme.typography.labelSmall,
                color = OnHero.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

/**
 * Band colours, cycled off the brand ramp.
 *
 * One hue in four steps rather than four hues: the segments are shares of one
 * total, and colouring them as unrelated categories would say otherwise.
 */
private fun compositionColor(index: Int): Color = when (index % 4) {
    0 -> Palette300
    1 -> Palette500
    2 -> Color(0xFF7FE3F5)
    else -> Color(0xFF4FA8C7)
}

// -----------------------------------------------------------------------------
// The list
// -----------------------------------------------------------------------------

/**
 * One account.
 *
 * Two rows rather than one. Everything used to fight for a single line - name,
 * type, share, balance and the archive control - and the loser was always the
 * subtitle, which ended as "Bank · 98% of what y...". Identity sits on the top
 * row and money on the bottom, which gives the balance room to be the size it
 * deserves and turns the share from a truncated sentence into a figure with a
 * track under it.
 *
 * A negative balance is shown in the negative colour whatever the account is:
 * on a card it is what is owed, on a current account it is an overdraft, and
 * neither should read as money available.
 */
@Composable
private fun AccountCard(
    account: Account,
    currencySymbol: String,
    share: Float?,
    canArchive: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val negative = account.balance.isNegative
    val accent = if (negative) MoneBieTheme.finance.negative else MaterialTheme.colorScheme.primary

    // The card opens the editor - renaming is the common case - and archiving
    // sits behind its own control so the two are not one mis-tap apart.
    PremiumCard(modifier = modifier.fillMaxWidth(), onClick = onEdit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = MoneBieIcons.forAccount(account.type.iconKey),
                tint = accent
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = account.type.displayName)
            }

            if (canArchive) {
                GlassIconButton(
                    icon = Icons.Rounded.Archive,
                    contentDescription = "Archive ${account.name}",
                    onClick = onArchive,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 34.dp
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MoneyText(
                    money = if (negative) account.balance.absolute else account.balance,
                    currencySymbol = currencySymbol,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (negative) {
                        MoneBieTheme.finance.negative
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    showDecimals = false
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = if (negative) "owed" else "available")
            }

            if (share != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MoneyFormatter.percent(share),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Spacer(Modifier.height(2.dp))
                    MicroLabel(text = "of what you hold")
                }
            }
        }

        if (share != null) {
            Spacer(Modifier.height(MoneBieTheme.spacing.small))

            GradientTrack(
                progress = share,
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                ),
                height = 6.dp
            )
        }
    }
}

/**
 * The end of the list, and a way to extend it.
 *
 * The floating button is the fast path for someone who already knows it is
 * there; this is for the person reading down the list and wondering whether it
 * is the whole of what they can have. Outlined rather than filled, so it reads
 * as a slot to fill rather than an account they own.
 */
@Composable
private fun AddAccountTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .pressable(onClick)
            .padding(MoneBieTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
    ) {
        GlowBadge(icon = Icons.Rounded.Add, tint = MaterialTheme.colorScheme.primary)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Add an account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "A bank, a wallet, cash in hand or a card you owe on",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** What a section of the list adds up to, sat on its header. */
@Composable
private fun SectionTotal(
    money: Money,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    MoneyText(
        money = money,
        currencySymbol = currencySymbol,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        showDecimals = false,
        modifier = modifier
    )
}

/** Skeleton in the shape of the real screen, so nothing jumps on first load. */
@Composable
private fun AccountsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 224.dp, cornerRadius = 30.dp)
        Spacer(Modifier.height(MoneBieTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 18.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 82.dp, cornerRadius = 24.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 82.dp, cornerRadius = 24.dp)
    }
}
