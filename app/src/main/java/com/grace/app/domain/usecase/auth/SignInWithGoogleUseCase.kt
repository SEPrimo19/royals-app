package com.grace.app.domain.usecase.auth

import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.signInWithGoogle()
}
