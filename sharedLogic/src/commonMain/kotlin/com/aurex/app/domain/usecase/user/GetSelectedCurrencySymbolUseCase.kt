package com.aurex.app.domain.usecase.user

import com.aurex.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetSelectedCurrencySymbolUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<String?> {
        return userRepository.getSelectedCurrencySymbol()
    }
}
