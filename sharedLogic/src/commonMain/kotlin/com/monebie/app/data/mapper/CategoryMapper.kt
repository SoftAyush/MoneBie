package com.aurex.app.data.mapper

import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.db.Category as DbCategory

fun DbCategory.toDomain(): TransactionCategory = TransactionCategory(
    id = id,
    name = name,
    iconKey = icon_key,
    appliesTo = applies_to.toTransactionType(),
    isBuiltIn = is_built_in == 1L,
    isProtected = is_protected == 1L,
    isArchived = is_archived == 1L,
    sortOrder = sort_order.toInt()
)

/**
 * Stored by name rather than ordinal so reordering the enum can never silently
 * reinterpret existing rows. An unknown name falls back to EXPENSE instead of
 * throwing, so a downgraded install still opens.
 */
private fun String.toTransactionType(): TransactionType =
    TransactionType.entries.firstOrNull { it.name == this } ?: TransactionType.EXPENSE
