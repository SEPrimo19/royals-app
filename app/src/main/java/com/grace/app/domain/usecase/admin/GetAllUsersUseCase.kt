package com.grace.app.domain.usecase.admin

import com.grace.app.domain.model.User
import com.grace.app.domain.repository.AdminRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetAllUsersUseCase @Inject constructor(
    private val adminRepository: AdminRepository
) {
    suspend operator fun invoke(): Result<List<User>> = adminRepository.getAllUsers()
}
