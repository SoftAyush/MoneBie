package com.aurex.app

import com.aurex.app.core.Money
import com.aurex.app.domain.model.Budget
import com.aurex.app.domain.model.BudgetLevel
import com.aurex.app.domain.model.CategoryLookup
import com.aurex.app.domain.model.Transaction
import com.aurex.app.domain.model.TransactionCategories
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.usecase.budget.BudgetMeasure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetMeasureTest {

    private fun budget(id: Long, categoryId: String, major: Long) =
        Budget(id = id, categoryId = categoryId, amount = Money.fromMajor(major), createdAt = "")

    private fun transaction(
        id: Long,
        major: Long,
        categoryId: String,
        type: TransactionType = TransactionType.EXPENSE
    ) = Transaction(
        id = id,
        amount = Money.fromMajor(major),
        type = type,
        categoryId = categoryId,
        accountId = 1L,
        date = "2026-09-04 10:00:00"
    )

    @Test
    fun spentIsTheSumOfExpensesInTheCategoryAndNothingElse() {
        val statuses = BudgetMeasure.measure(
            budgets = listOf(budget(1L, "food", 300L)),
            transactions = listOf(
                transaction(1L, 80L, "food"),
                transaction(2L, 40L, "food"),
                transaction(3L, 500L, "housing"),
                // A refund filed under food is income and does not give room back.
                transaction(4L, 30L, "food", type = TransactionType.INCOME)
            ),
            categories = CategoryLookup.Seeded
        )

        val food = statuses.single()
        assertEquals(Money.fromMajor(120L), food.spent)
        assertEquals(Money.fromMajor(180L), food.remaining)
        assertEquals(2, food.transactionCount)
        assertEquals(BudgetLevel.OK, food.level)
        assertEquals(0.4f, food.progress)
    }

    @Test
    fun levelsFollowTheShareUsed() {
        val statuses = BudgetMeasure.measure(
            budgets = listOf(
                budget(1L, "food", 100L),
                budget(2L, "transport", 100L),
                budget(3L, "shopping", 100L)
            ),
            transactions = listOf(
                transaction(1L, 50L, "food"),
                transaction(2L, 85L, "transport"),
                transaction(3L, 130L, "shopping")
            ),
            categories = CategoryLookup.Seeded
        )

        val byCategory = statuses.associateBy { it.budget.categoryId }
        assertEquals(BudgetLevel.OK, byCategory.getValue("food").level)
        assertEquals(BudgetLevel.NEAR, byCategory.getValue("transport").level)
        assertEquals(BudgetLevel.OVER, byCategory.getValue("shopping").level)

        val over = byCategory.getValue("shopping")
        assertTrue(over.remaining.isNegative)
        assertEquals(1f, over.progress, "progress saturates rather than exceeding the bar")
        assertEquals(1.3f, over.usedRatio)
    }

    @Test
    fun worstBudgetsComeFirst() {
        val statuses = BudgetMeasure.measure(
            budgets = listOf(
                budget(1L, "food", 100L),
                budget(2L, "transport", 100L),
                budget(3L, "shopping", 100L)
            ),
            transactions = listOf(
                transaction(1L, 10L, "food"),
                transaction(2L, 150L, "transport"),
                transaction(3L, 90L, "shopping")
            ),
            categories = CategoryLookup.Seeded
        )

        assertEquals(listOf("transport", "shopping", "food"), statuses.map { it.budget.categoryId })
    }

    @Test
    fun aBudgetWithNothingSpentIsSimplyUntouched() {
        val status = BudgetMeasure.measure(
            budgets = listOf(budget(1L, "food", 100L)),
            transactions = emptyList(),
            categories = CategoryLookup.Seeded
        ).single()

        assertEquals(Money.ZERO, status.spent)
        assertFalse(status.isOver)
        assertEquals(0f, status.progress)
    }

    @Test
    fun onlyActiveExpenseCategoriesWithoutABudgetAreOffered() {
        val offered = BudgetMeasure.unbudgeted(
            categories = TransactionCategories.all,
            budgets = listOf(budget(1L, "food", 100L))
        )

        assertTrue(offered.none { it.id == "food" })
        assertTrue(offered.none { it.appliesTo != TransactionType.EXPENSE })
        assertTrue(offered.any { it.id == "groceries" })
    }
}
