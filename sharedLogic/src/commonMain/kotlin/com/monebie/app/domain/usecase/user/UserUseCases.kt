package com.monebie.app.domain.usecase.user

import com.monebie.app.domain.model.ThemeMode
import com.monebie.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

/** The appearance setting, so the whole app can be themed from one source. */
class GetThemeModeUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<ThemeMode> = userRepository.observeThemeMode()
}

class SetThemeModeUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(mode: ThemeMode) = userRepository.setThemeMode(mode)
}
