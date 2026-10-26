package com.monebie.app.domain.usecase.user

import com.monebie.app.domain.model.User
import com.monebie.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetUsersUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<List<User>> {
        return userRepository.getAllUsers()
    }
}
