package com.grace.app.domain.usecase.auth

import com.grace.app.domain.model.User
import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) {
            return Result.Error("Please enter your email address.")
        }
        if (!isValidEmail(email)) {
            return Result.Error("Please enter a valid email address.")
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.Error("Password must be at least $MIN_PASSWORD_LENGTH characters.")
        }
        return authRepository.signIn(email.trim(), password)
    }
}
