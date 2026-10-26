package com.monebie.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import com.monebie.app.domain.usecase.onboarding.GetOnboardingPagesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(
    private val getOnboardingPagesUseCase: GetOnboardingPagesUseCase = GetOnboardingPagesUseCase()
) : ViewModel() {

    private val pages = getOnboardingPagesUseCase()

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            currentPageIndex = 0,
            currentPage = pages.firstOrNull(),
            pages = pages,
            totalPages = pages.size
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Advances one page, or finishes when there is nothing left to show.
     *
     * [onFinished] is invoked outside the state update: `update` re-runs its
     * lambda when another writer wins the compare-and-set, which would navigate
     * twice.
     */
    fun nextStep(onFinished: () -> Unit) {
        val nextIndex = _uiState.value.currentPageIndex + 1
        if (nextIndex < pages.size) {
            goToPage(nextIndex)
        } else {
            finish(onFinished)
        }
    }

    /**
     * Moves straight to [index], for the swipeable pager driving the screen.
     * Out-of-range values are ignored so the caller need not bounds-check.
     */
    fun goToPage(index: Int) {
        if (index !in pages.indices) return
        _uiState.update { state ->
            if (state.currentPageIndex == index) {
                state
            } else {
                state.copy(currentPageIndex = index, currentPage = pages[index])
            }
        }
    }

    /** Leaves onboarding without walking the remaining pages. */
    fun skip(onFinished: () -> Unit) = finish(onFinished)

    /**
     * Leaves the flow.
     *
     * Deliberately not latched on [OnboardingUiState.isFinished]: this view model
     * is scoped to the nav back stack entry, so it survives a trip forward and
     * back to onboarding. A latch here would leave Skip and Next dead on return.
     * Duplicate navigation is handled at the call site with launchSingleTop.
     */
    private fun finish(onFinished: () -> Unit) {
        _uiState.update { it.copy(isFinished = true) }
        onFinished()
    }
}
