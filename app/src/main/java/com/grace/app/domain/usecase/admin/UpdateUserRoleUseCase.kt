package com.grace.app.domain.usecase.admin

import com.grace.app.domain.model.UserRole
import com.grace.app.domain.repository.AdminRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class UpdateUserRoleUseCase @Inject constructor(
    private val adminRepository: AdminRepository
) {
    suspend operator fun invoke(targetUserId: String, newRole: UserRole): Result<Unit> =
        adminRepository.updateUserRole(targetUserId, newRole)
}
