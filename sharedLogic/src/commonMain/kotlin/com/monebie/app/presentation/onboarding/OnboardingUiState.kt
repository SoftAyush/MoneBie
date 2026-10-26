package com.monebie.app.presentation.onboarding

import com.monebie.app.domain.model.OnboardingPage

data class OnboardingUiState(
    val currentPageIndex: Int = 0,
    val currentPage: OnboardingPage? = null,
    /** Every page, so a swipeable pager can render neighbours mid-drag. */
    val pages: List<OnboardingPage> = emptyList(),
    val totalPages: Int = 0,
    val isFinished: Boolean = false
)
