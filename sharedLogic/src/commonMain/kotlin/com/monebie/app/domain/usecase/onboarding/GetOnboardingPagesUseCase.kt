package com.aurex.app.domain.usecase.onboarding

import com.aurex.app.domain.model.OnboardingPage

class GetOnboardingPagesUseCase {
    operator fun invoke(): List<OnboardingPage> {
        return listOf(
            OnboardingPage(
                title = "Master Your Money",
                description = "Track every penny, manage debts, and plan your future with confidence.",
                imageResName = "jumping_coins"
            ),
            OnboardingPage(
                title = "Simplify Your Debts",
                description = "Keep track of what you owe and what's owed to you in one secure place.",
                imageResName = "agreement_debt"
            ),
            OnboardingPage(
                title = "Never Miss a Payment",
                description = "Smart reminders and progress tracking for all your active loans and EMIs.",
                imageResName = "payment_reminder"
            )
        )
    }
}
