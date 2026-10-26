package com.aurex.app.domain.usecase.user

import com.aurex.app.domain.model.User
import com.aurex.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The single profile row, or null before profile setup completes. */
class GetCurrentUserUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<User?> =
        userRepository.getAllUsers().map { it.firstOrNull() }
}
