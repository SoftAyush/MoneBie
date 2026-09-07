package com.aurex.app.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.presentation.category.CategoriesViewModel
import com.aurex.app.presentation.category.CategoryEvent
import com.aurex.app.presentation.category.CategoryFormState
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexButton
import com.aurex.app.ui.components.AurexIcons
import com.aurex.app.ui.components.AurexSnackbarHost
import com.aurex.app.ui.components.AurexTextField
import com.aurex.app.ui.components.ConfirmDialog
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.FilterPill
import com.aurex.app.ui.components.FormField
import com.aurex.app.ui.components.GlassIconButton
import com.aurex.app.ui.components.GlowBadge
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroFinance
import com.aurex.app.ui.components.HeroNote
import com.aurex.app.ui.components.HeroPill
import com.aurex.app.ui.components.HeroSplitBar
import com.aurex.app.ui.components.HeroTile
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.OnHero
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.PremiumSectionHeader
import com.aurex.app.ui.components.Segment
import com.aurex.app.ui.components.SegmentedSelector
import com.aurex.app.ui.components.ShimmerBlock
import com.aurex.app.ui.components.entrance
import com.aurex.app.ui.components.heroBrush
import com.aurex.app.ui.components.pressable
import com.aurex.app.ui.components.rememberEntrance
import com.aurex.app.ui.components.showAurexMessage
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * Category management.
 *
 * Laid out as a two-column board rather than a list. A category is an icon and
 * a short name - the shape a tile is made for - and the board shows roughly
 * twice as many at once, which is what turns "where is Groceries" from a scroll
 * into a glance.
 *
 * The two sides are still shown apart because a category belongs to one of them
 * for life - what a transaction is filed under decides which column of the
 * ledger it lands in, so moving one across afterwards would rewrite history.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: CategoriesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CategoriesViewModel(
                    getCategoriesUseCase = app.appContainer.getCategoriesUseCase,
                    saveCategoryUseCase = app.appContainer.saveCategoryUseCase,
                    removeCategoryUseCase = app.appContainer.removeCategoryUseCase,
                    restoreCategoryUseCase = app.appContainer.restoreCategoryUseCase,
                    countCategoryUsageUseCase = app.appContainer.countCategoryUsageUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val pendingUsage by viewModel.pendingRemovalUsage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRemoval by remember { mutableStateOf<TransactionCategory?>(null) }

    // What the sheet is editing. The form state carries only an id, and the
    // removal control inside the sheet needs the row itself.
    var editing by remember { mutableStateOf<TransactionCategory?>(null) }

    var lens by rememberSaveable { mutableStateOf(CategoryLens.ALL) }
    var query by rememberSaveable { mutableStateOf("") }

    // Restoring the last hidden category takes its filter off the row with it.
    // Left alone, the board would sit empty behind a lens the user can no
    // longer see is selected.
    val nothingHidden = uiState.hidden.isEmpty()
    LaunchedEffect(nothingHidden) {
        if (nothingHidden && lens == CategoryLens.HIDDEN) lens = CategoryLens.ALL
    }

    LaunchedEffect(event) {
        when (val current = event) {
            is CategoryEvent.Saved -> snackbarHostState.showAurexMessage("Saved ${current.name}.")
            is CategoryEvent.Deleted -> snackbarHostState.showAurexMessage("Deleted ${current.name}.")
            is CategoryEvent.Hidden -> snackbarHostState.showAurexMessage(
                current.name + " is hidden. Its " +
                    (
                        if (current.transactionCount == 1) {
                            "1 transaction keeps"
                        } else {
                            "${current.transactionCount} transactions keep"
                        }
                        ) + " the label."
            )

            is CategoryEvent.Restored -> snackbarHostState.showAurexMessage("${current.name} is back.")
            is CategoryEvent.Refused -> snackbarHostState.showAurexMessage(current.message)
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    val term = query.trim()

    fun List<TransactionCategory>.matching(): List<TransactionCategory> =
        if (term.isEmpty()) this else filter { it.name.contains(term, ignoreCase = true) }

    val expense = uiState.expense.matching()
    val income = uiState.income.matching()
    val hidden = uiState.hidden.matching()

    val showExpense = lens == CategoryLens.ALL || lens == CategoryLens.OUT
    val showIncome = lens == CategoryLens.ALL || lens == CategoryLens.IN
    val showHidden = (lens == CategoryLens.ALL || lens == CategoryLens.HIDDEN) && hidden.isNotEmpty()
    val nothingMatches = term.isNotEmpty() &&
        expense.isEmpty() && income.isEmpty() && hidden.isEmpty()

    val negative = AurexTheme.finance.negative
    val positive = AurexTheme.finance.positive

    val openEditor: (TransactionCategory) -> Unit = { category ->
        editing = category
        viewModel.startEditing(category)
    }
    val openCreator: (TransactionType) -> Unit = { type ->
        editing = null
        viewModel.startCreating(type)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Categories",
                onBack = onBack,
                subtitle = when {
                    uiState.isLoading -> "Loading"
                    uiState.customCount == 1 -> "1 of your own"
                    uiState.customCount > 1 -> "${uiState.customCount} of your own"
                    else -> "Built-in set"
                },
                actions = {
                    GlassIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "New category",
                        onClick = {
                            openCreator(
                                if (lens == CategoryLens.IN) {
                                    TransactionType.INCOME
                                } else {
                                    TransactionType.EXPENSE
                                }
                            )
                        }
                    )
                }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AurexTheme.spacing.medium,
                    end = AurexTheme.spacing.medium,
                    top = AurexTheme.spacing.small,
                    bottom = AurexTheme.spacing.extraExtraLarge
                ),
                horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
            ) {
                item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                    CategoryHero(
                        expenseCount = uiState.expense.size,
                        incomeCount = uiState.income.size,
                        customCount = uiState.customCount,
                        hiddenCount = uiState.hidden.size,
                        loading = uiState.isLoading
                    )
                }

                if (uiState.isLoading) {
                    items(6) { CategoryTileSkeleton() }
                    return@LazyVerticalGrid
                }

                item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                    CategorySearchField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.padding(top = AurexTheme.spacing.extraSmall)
                    )
                }

                item(key = "lenses", span = { GridItemSpan(maxLineSpan) }) {
                    LensRow(
                        lens = lens,
                        onSelect = { lens = it },
                        expenseCount = expense.size,
                        incomeCount = income.size,
                        hiddenCount = hidden.size,
                        negative = negative,
                        positive = positive
                    )
                }

                if (nothingMatches) {
                    item(key = "no_match", span = { GridItemSpan(maxLineSpan) }) {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.Search,
                                title = "Nothing called \"$term\"",
                                message = "Try a shorter word, or add a category of your own " +
                                    "under that name.",
                                actionLabel = "Add a category",
                                onAction = { openCreator(TransactionType.EXPENSE) }
                            )
                        }
                    }
                }

                if (showExpense && !nothingMatches) {
                    categorySection(
                        id = "expense",
                        title = "Money out",
                        accent = negative,
                        categories = expense,
                        searching = term.isNotEmpty(),
                        onAdd = { openCreator(TransactionType.EXPENSE) },
                        onEdit = openEditor
                    )
                }

                if (showIncome && !nothingMatches) {
                    categorySection(
                        id = "income",
                        title = "Money in",
                        accent = positive,
                        categories = income,
                        searching = term.isNotEmpty(),
                        onAdd = { openCreator(TransactionType.INCOME) },
                        onEdit = openEditor
                    )
                }

                if (showHidden) {
                    item(key = "hidden_header", span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(top = AurexTheme.spacing.small)) {
                            PremiumSectionHeader(title = "Hidden")
                            Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
                            Text(
                                text = "Not offered when recording, but still naming the " +
                                    "transactions already filed under them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = hidden,
                        key = { "hidden_" + it.id },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { category ->
                        HiddenCategoryCard(
                            category = category,
                            onRestore = { viewModel.restore(category) }
                        )
                    }
                }
            }
        }

        /*
         * No floating button.
         *
         * Adding sits in three places the eye is already on - the bar, each
         * side's header, and the dashed tile that closes each side - and a
         * button hovering over a two-column board would cover a tile wherever
         * it landed.
         */

        AurexSnackbarHost(
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
            CategoryFormSheet(
                form = form,
                // Removal lives in the editor rather than on every tile: the
                // board would otherwise carry two dozen delete buttons, each
                // one mis-tap away from the category it belongs to.
                removable = editing?.takeIf { form.isEditing && !it.isProtected },
                onNameChange = viewModel::setName,
                onIconChange = viewModel::setIcon,
                onSideChange = viewModel::setAppliesTo,
                onSave = viewModel::save,
                onRemove = { category ->
                    /*
                     * The sheet stays open under the confirmation.
                     *
                     * Closing it here would tear its window down in the same
                     * frame the dialog's window goes up, and a second tap
                     * arriving mid-handoff leaves the dialog on screen but deaf
                     * - taps land on the board behind it. Cancelling returns to
                     * the editor, which is where the user was anyway.
                     */
                    if (pendingRemoval == null) {
                        pendingRemoval = category
                        viewModel.prepareRemoval(category)
                    }
                }
            )
        }
    }

    pendingRemoval?.let { category ->
        val usage = pendingUsage
        ConfirmDialog(
            title = if (category.isBuiltIn) {
                "Hide ${category.name}?"
            } else {
                "Delete ${category.name}?"
            },
            // The count decides what actually happens, so the copy waits for it
            // rather than promising one outcome and performing the other.
            message = when {
                usage == null -> "Checking what is filed under it..."
                usage > 0 -> "It is used by " +
                    (if (usage == 1) "1 transaction" else "$usage transactions") +
                    ", so it will be hidden instead of deleted and that history keeps its label."

                category.isBuiltIn -> "Nothing is filed under it. It will be hidden from the " +
                    "pickers and you can bring it back at any time."

                else -> "Nothing is filed under it, so it will be deleted outright."
            },
            confirmLabel = if (category.isBuiltIn || (usage ?: 0) > 0) "Hide" else "Delete",
            isDestructive = true,
            // This one opens from a button inside a sheet that closes in the
            // same frame, so the scrim arrives under a finger that may still be
            // tapping. Cancel and the back gesture stay; a stray tap does not
            // count as either.
            dismissOnClickOutside = false,
            onConfirm = {
                viewModel.remove(category)
                pendingRemoval = null
                // Whatever raised this - a tile's editor or nothing at all -
                // the editor has nothing left to edit once it is gone.
                viewModel.dismissForm()
            },
            onDismiss = {
                pendingRemoval = null
                viewModel.clearPendingRemoval()
            }
        )
    }
}

// -----------------------------------------------------------------------------
// The board
// -----------------------------------------------------------------------------

/** Which slice of the board is on show. */
private enum class CategoryLens { ALL, OUT, IN, HIDDEN }

/** Every tile is the same height, so the board keeps its grid however long a name is. */
private val TileHeight = 138.dp

private val TileShape = RoundedCornerShape(22.dp)

/** One side of the ledger: its header, its tiles, and the slot that extends it. */
private fun LazyGridScope.categorySection(
    id: String,
    title: String,
    accent: Color,
    categories: List<TransactionCategory>,
    searching: Boolean,
    onAdd: () -> Unit,
    onEdit: (TransactionCategory) -> Unit
) {
    // A side with no match drops out of a search entirely. Its pill already
    // reads zero, and a header over an empty card would push the matches the
    // search was for off the bottom of the screen.
    if (categories.isEmpty() && searching) return

    item(key = id + "_header", span = { GridItemSpan(maxLineSpan) }) {
        SideHeader(title = title, count = categories.size, accent = accent, onAdd = onAdd)
    }

    if (categories.isEmpty()) {
        item(key = id + "_empty", span = { GridItemSpan(maxLineSpan) }) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    icon = Icons.Rounded.Sell,
                    title = "Nothing on this side",
                    message = "Every category here has been hidden. Add one, or restore a " +
                        "hidden one below.",
                    actionLabel = "Add one",
                    onAction = onAdd
                )
            }
        }
        return
    }

    items(items = categories, key = { id + "_" + it.id }) { category ->
        CategoryTile(
            category = category,
            accent = accent,
            onClick = { onEdit(category) }
        )
    }

    item(key = id + "_add") {
        AddCategoryTile(label = title, accent = accent, onClick = onAdd)
    }
}

/**
 * One category, as a tile.
 *
 * The whole tile opens the editor - renaming and re-iconing is the only thing
 * most people ever do here - and what is left to say about the row is carried
 * by a mark in the corner rather than a second control.
 */
@Composable
private fun CategoryTile(
    category: TransactionCategory,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(
        modifier = modifier
            .fillMaxWidth()
            .height(TileHeight),
        contentPadding = 14.dp,
        shape = TileShape,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            GlowBadge(
                icon = AurexIcons.forCategory(category.iconKey),
                tint = accent,
                size = 40.dp
            )

            Spacer(Modifier.weight(1f))

            when {
                // Nothing to offer but the reason: this one cannot be removed.
                category.isProtected -> Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Aurex posts to this category",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(15.dp)
                )

                // A quiet mark of ownership, which costs less room than a word.
                !category.isBuiltIn -> Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )

                else -> Unit
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = category.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(3.dp))

        MicroLabel(
            text = when {
                category.isProtected -> "Reserved"
                category.isBuiltIn -> "Built in"
                else -> "Yours"
            }
        )
    }
}

/**
 * The slot at the end of a side.
 *
 * Dashed rather than filled, so it reads as a space to fill rather than a
 * category that already exists.
 */
@Composable
private fun AddCategoryTile(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(TileHeight)
            .clip(TileShape)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(9.dp.toPx(), 7.dp.toPx())
                        )
                    )
                )
            }
            .pressable(onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlowBadge(icon = Icons.Rounded.Add, tint = accent, size = 40.dp)

        Spacer(Modifier.height(10.dp))

        Text(
            text = "New category",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        Spacer(Modifier.height(2.dp))

        MicroLabel(text = label)
    }
}

/** A tile's shape while the first database read resolves. */
@Composable
private fun CategoryTileSkeleton(modifier: Modifier = Modifier) {
    PremiumCard(
        modifier = modifier
            .fillMaxWidth()
            .height(TileHeight),
        contentPadding = 14.dp,
        shape = TileShape
    ) {
        ShimmerBlock(modifier = Modifier.size(40.dp), height = 40.dp, cornerRadius = 20.dp)
        Spacer(Modifier.weight(1f))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.75f), height = 14.dp)
        Spacer(Modifier.height(6.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 10.dp)
    }
}

// -----------------------------------------------------------------------------
// The hero
// -----------------------------------------------------------------------------

/**
 * What the pickers are actually offering.
 *
 * The figure is how many are in use, and the bar under it is the point: a set
 * that is twenty ways to spend and two ways to earn is a set that will file
 * most of its income under "Other".
 */
@Composable
private fun CategoryHero(
    expenseCount: Int,
    incomeCount: Int,
    customCount: Int,
    hiddenCount: Int,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val entrance = rememberEntrance()
    val active = expenseCount + incomeCount

    Box(
        modifier = modifier
            .fillMaxWidth()
            .entrance(entrance)
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
                MicroLabel(text = "In your pickers", color = OnHero.copy(alpha = 0.75f))
                Spacer(Modifier.weight(1f))
                HeroPill(
                    text = when {
                        customCount == 0 -> "Built-in set"
                        customCount == 1 -> "1 yours"
                        else -> "$customCount yours"
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (loading) "--" else active.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = OnHero
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (active == 1) "category" else "categories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnHero.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            HeroSplitBar(
                leftValue = expenseCount.toLong(),
                rightValue = incomeCount.toLong(),
                leftColor = HeroFinance.negative,
                rightColor = HeroFinance.positive
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Money out",
                    value = expenseCount.toString(),
                    valueColor = HeroFinance.negative,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Money in",
                    value = incomeCount.toString(),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
            }

            if (hiddenCount > 0) {
                Spacer(Modifier.height(10.dp))

                HeroNote(
                    icon = Icons.Rounded.VisibilityOff,
                    text = (
                        if (hiddenCount == 1) {
                            "1 category is"
                        } else {
                            "$hiddenCount categories are"
                        }
                        ) + " hidden - still naming what is already filed under them."
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Narrowing the board
// -----------------------------------------------------------------------------

/** Search across both sides at once, and across what is hidden. */
@Composable
private fun CategorySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = scheme.onSurface,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(scheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(scheme.surface.copy(alpha = 0.9f))
                    .border(
                        width = 1.dp,
                        color = if (focused) {
                            scheme.primary.copy(alpha = 0.55f)
                        } else {
                            scheme.outline.copy(alpha = 0.20f)
                        },
                        shape = CircleShape
                    )
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 14.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search categories",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }

                if (value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .pressable { onValueChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear the search",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(10.dp))
                }
            }
        }
    )
}

/**
 * All, one side, or what is hidden - with what each would leave on screen.
 *
 * The counts follow the search rather than the whole set, so a filter that
 * leads nowhere says so before it is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LensRow(
    lens: CategoryLens,
    onSelect: (CategoryLens) -> Unit,
    expenseCount: Int,
    incomeCount: Int,
    hiddenCount: Int,
    negative: Color,
    positive: Color,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AurexTheme.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        FilterPill(
            label = "All",
            selected = lens == CategoryLens.ALL,
            onClick = { onSelect(CategoryLens.ALL) },
            count = expenseCount + incomeCount
        )
        FilterPill(
            label = "Money out",
            selected = lens == CategoryLens.OUT,
            onClick = { onSelect(CategoryLens.OUT) },
            tint = negative,
            count = expenseCount
        )
        FilterPill(
            label = "Money in",
            selected = lens == CategoryLens.IN,
            onClick = { onSelect(CategoryLens.IN) },
            tint = positive,
            count = incomeCount
        )
        if (hiddenCount > 0) {
            FilterPill(
                label = "Hidden",
                selected = lens == CategoryLens.HIDDEN,
                onClick = { onSelect(CategoryLens.HIDDEN) },
                icon = Icons.Rounded.VisibilityOff,
                count = hiddenCount
            )
        }
    }
}

@Composable
private fun SideHeader(
    title: String,
    count: Int,
    accent: Color,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AurexTheme.spacing.small, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.35f)))
                )
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )

        Spacer(Modifier.weight(1f))

        GlassIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Add a category to $title",
            onClick = onAdd,
            tint = accent,
            size = 34.dp
        )
    }
}

@Composable
private fun HiddenCategoryCard(
    category: TransactionCategory,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val side = if (category.appliesTo == TransactionType.INCOME) {
        AurexTheme.finance.positive
    } else {
        AurexTheme.finance.negative
    }

    PremiumCard(modifier = modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = AurexIcons.forCategory(category.iconKey),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 38.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(side.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.width(6.dp))
                    MicroLabel(
                        text = if (category.appliesTo == TransactionType.INCOME) {
                            "Money in"
                        } else {
                            "Money out"
                        }
                    )
                }
            }

            GlassIconButton(
                icon = Icons.Rounded.Restore,
                contentDescription = "Restore ${category.name}",
                onClick = onRestore,
                size = 34.dp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// The sheet
// -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFormSheet(
    form: CategoryFormState,
    removable: TransactionCategory?,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onSideChange: (TransactionType) -> Unit,
    onSave: () -> Unit,
    onRemove: (TransactionCategory) -> Unit
) {
    val accent = if (form.appliesTo == TransactionType.INCOME) {
        AurexTheme.finance.positive
    } else {
        AurexTheme.finance.negative
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AurexTheme.spacing.medium)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The badge previews the choice: icon and side at the size they
            // will appear at in the picker this is being built for.
            GlowBadge(icon = AurexIcons.forCategory(form.iconKey), tint = accent)
            Spacer(Modifier.width(AurexTheme.spacing.medium))
            Column {
                Text(
                    text = if (form.isEditing) "Edit category" else "New category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(
                    text = form.name.trim().ifEmpty {
                        if (form.appliesTo == TransactionType.INCOME) "Money in" else "Money out"
                    }
                )
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        if (form.canChangeSide) {
            FormField(label = "Side of the ledger") {
                SegmentedSelector(
                    segments = listOf(
                        Segment(
                            value = TransactionType.EXPENSE,
                            label = "Money out",
                            accent = AurexTheme.finance.negative
                        ),
                        Segment(
                            value = TransactionType.INCOME,
                            label = "Money in",
                            accent = AurexTheme.finance.positive
                        )
                    ),
                    selected = form.appliesTo,
                    onSelect = onSideChange
                )
            }

            Spacer(Modifier.height(AurexTheme.spacing.large))
        }

        FormField(
            label = "Name",
            error = form.nameError,
            hint = if (form.isEditing) {
                "Renaming updates every transaction already filed under it."
            } else {
                null
            }
        ) {
            AurexTextField(
                value = form.name,
                onValueChange = onNameChange,
                placeholder = "e.g. Coffee",
                leadingIcon = Icons.Rounded.Sell,
                textStyle = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FormField(label = "Icon") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
            ) {
                AurexIcons.categoryChoices.forEach { key ->
                    IconChoice(
                        iconKey = key,
                        selected = key == form.iconKey,
                        accent = accent,
                        onClick = { onIconChange(key) }
                    )
                }
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.extraLarge))

        AurexButton(
            text = if (form.isEditing) "Save changes" else "Add category",
            loading = form.isSaving,
            onClick = onSave
        )

        if (removable != null) {
            Spacer(Modifier.height(AurexTheme.spacing.small))

            RemoveCategoryButton(
                label = if (removable.isBuiltIn) {
                    "Hide this category"
                } else {
                    "Delete this category"
                },
                onClick = { onRemove(removable) }
            )
        }

        Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
    }
}

/**
 * The way out of the editor.
 *
 * Ghosted rather than filled: it sits directly under the save button, and two
 * solid buttons in a row would give equal weight to keeping and to destroying.
 */
@Composable
private fun RemoveCategoryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val error = MaterialTheme.colorScheme.error
    val shape = MaterialTheme.shapes.large

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(error.copy(alpha = 0.08f))
            .border(1.dp, error.copy(alpha = 0.22f), shape)
            .pressable(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = null,
            tint = error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = error
        )
    }
}

/**
 * One icon in the picker.
 *
 * A rounded square rather than a circle: two dozen of them tile into a board
 * that reads as a keypad, where circles read as scattered dots.
 */
@Composable
private fun IconChoice(
    iconKey: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.12f))
                        )
                    )
                } else {
                    Modifier.background(scheme.surfaceContainer)
                }
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else scheme.outline.copy(alpha = 0.20f),
                shape = shape
            )
            .pressable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AurexIcons.forCategory(iconKey),
            contentDescription = iconKey,
            tint = if (selected) accent else scheme.onSurfaceVariant,
            modifier = Modifier.size(23.dp)
        )
    }
}
