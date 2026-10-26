package com.aurex.app.domain.usecase.user

import com.aurex.app.domain.repository.UserRepository

class SaveUserUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(name: String, date: String, currencyCode: String) {
        userRepository.insertUser(name, date, currencyCode)
    }
}
