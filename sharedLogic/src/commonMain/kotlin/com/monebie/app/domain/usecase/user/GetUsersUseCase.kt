package com.aurex.app.domain.usecase.user

import com.aurex.app.domain.model.User
import com.aurex.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetUsersUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<List<User>> {
        return userRepository.getAllUsers()
    }
}
