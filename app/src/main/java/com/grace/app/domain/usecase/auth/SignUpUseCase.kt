package com.grace.app.domain.usecase.auth

import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Result<Unit> {
        if (name.trim().length < MIN_NAME_LENGTH) {
            return Result.Error("Please enter your name.")
        }
        if (!isValidEmail(email)) {
            return Result.Error("Please enter a valid email address.")
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.Error("Password must be at least $MIN_PASSWORD_LENGTH characters.")
        }
        if (password != confirmPassword) {
            return Result.Error("Passwords do not match.")
        }
        return authRepository.signUp(email.trim(), password, name.trim())
    }
}
