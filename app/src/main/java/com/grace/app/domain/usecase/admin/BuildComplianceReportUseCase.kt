package com.grace.app.domain.usecase.admin

import com.grace.app.domain.model.AdminAttendanceRecord
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.User
import com.grace.app.domain.repository.AdminRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

data class UserComplianceRow(
    val user: User,
    val attendanceCount: Int,
    val presentCount: Int,
    val lateCount: Int,
    val reflectionCount: Int,
    val isCompliant: Boolean,
    val lastAttendedAt: java.time.LocalDateTime?,
    val lastReflectionAt: java.time.LocalDateTime?
)

data class ComplianceReportData(
    val rows: List<UserComplianceRow>,
    val totalUsers: Int,
    val compliantCount: Int,
    val attendingCount: Int,
    val reflectingCount: Int
)

enum class ComplianceAudience { ALL, COMPASSION_ONLY, NON_COMPASSION }

class BuildComplianceReportUseCase @Inject constructor(
    private val adminRepository: AdminRepository
) {
    suspend operator fun invoke(
        audience: ComplianceAudience,
        periodIncludes: (java.time.LocalDateTime) -> Boolean
    ): Result<ComplianceReportData> {
        val usersResult = adminRepository.getAllUsers()
        if (usersResult is Result.Error) return usersResult
        val attendanceResult = adminRepository.getAllAttendance()
        val submissionsResult = adminRepository.getAllMeditationSubmissions()

        val users = (usersResult as Result.Success).data
            .filter { user ->
                when (audience) {
                    ComplianceAudience.ALL -> true
                    ComplianceAudience.COMPASSION_ONLY -> user.isCompassion
                    ComplianceAudience.NON_COMPASSION -> !user.isCompassion
                }
            }
        val attendance: List<AdminAttendanceRecord> =
            (attendanceResult as? Result.Success)?.data.orEmpty()
                .filter { periodIncludes(it.attendedAt) }
        val submissions: List<MeditationSubmission> =
            (submissionsResult as? Result.Success)?.data.orEmpty()
                .filter { periodIncludes(it.submittedAt) }

        val attendanceByUser = attendance.groupBy { it.userId }
        val submissionsByUser = submissions.groupBy { it.userId }

        val rows = users.map { user ->
            val myAttendance = attendanceByUser[user.id].orEmpty()
            val mySubs = submissionsByUser[user.id].orEmpty()
            val present = myAttendance.count { it.status == AttendanceStatus.PRESENT }
            val late = myAttendance.count { it.status == AttendanceStatus.LATE }
            UserComplianceRow(
                user = user,
                attendanceCount = myAttendance.size,
                presentCount = present,
                lateCount = late,
                reflectionCount = mySubs.size,
                isCompliant = myAttendance.isNotEmpty() && mySubs.isNotEmpty(),
                lastAttendedAt = myAttendance.maxByOrNull { it.attendedAt }
                    ?.attendedAt,
                lastReflectionAt = mySubs.maxByOrNull { it.submittedAt }
                    ?.submittedAt
            )
        }.sortedWith(
            compareByDescending<UserComplianceRow> { it.isCompliant }
                .thenByDescending { it.attendanceCount + it.reflectionCount }
                .thenBy { it.user.name }
        )

        return Result.Success(
            ComplianceReportData(
                rows = rows,
                totalUsers = users.size,
                compliantCount = rows.count { it.isCompliant },
                attendingCount = rows.count { it.attendanceCount > 0 },
                reflectingCount = rows.count { it.reflectionCount > 0 }
            )
        )
    }
}
