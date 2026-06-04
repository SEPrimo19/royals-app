package com.grace.app.domain.usecase.admin

import com.grace.app.domain.model.AdminAttendanceRecord
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.User
import com.grace.app.domain.repository.AdminRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

/**
 * Per-user roster entry on the admin compliance report. Pre-aggregated so
 * the PDF builder only iterates this list once and doesn't re-scan the
 * raw attendance / meditation lists per user.
 */
data class UserComplianceRow(
    val user: User,
    val attendanceCount: Int,
    val presentCount: Int,
    val lateCount: Int,
    val reflectionCount: Int,
    /** Compliant when the user has ≥1 attendance AND ≥1 reflection in period. */
    val isCompliant: Boolean,
    /** Convenience for risk shading. Null = no records at all in period. */
    val lastAttendedAt: java.time.LocalDateTime?,
    val lastReflectionAt: java.time.LocalDateTime?
)

data class ComplianceReportData(
    val rows: List<UserComplianceRow>,
    val totalUsers: Int,
    val compliantCount: Int,
    val attendingCount: Int,        // ≥1 attendance in period
    val reflectingCount: Int        // ≥1 reflection in period
)

/** Audience filter used by the compliance report. */
enum class ComplianceAudience { ALL, COMPASSION_ONLY, NON_COMPASSION }

/**
 * Pulls the three bulk lists (users, attendance, submissions) and folds
 * them into a per-user roster, applying the period predicate as it goes.
 *
 * Failures inside the bulk fetches don't blow up the report — we fall back
 * to empty lists for the failing source so the rest of the data still
 * renders. The admin can re-run if a section comes back unexpectedly bare.
 *
 * @param periodIncludes predicate the caller defines (typically
 * `ExportPeriod.includes`) so the use case doesn't depend on a UI type.
 */
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

        // Bucket by userId once — cheaper than re-scanning the full lists
        // per user when the rosters get large.
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
            // Most-engaged first: compliant before non-compliant, then by
            // total activity (attendance + reflections), then alphabetical.
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
