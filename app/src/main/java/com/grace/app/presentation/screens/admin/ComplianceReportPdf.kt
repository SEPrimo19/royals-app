package com.grace.app.presentation.screens.admin

import com.grace.app.data.util.PdfExporter
import com.grace.app.domain.usecase.admin.ComplianceAudience
import com.grace.app.domain.usecase.admin.ComplianceReportData
import com.grace.app.domain.usecase.admin.UserComplianceRow
import com.grace.app.presentation.screens.progress.ExportPeriod

private const val COLOR_PRESENT = 0xFF3ECF8E.toInt()
private const val COLOR_LATE    = 0xFFF4A261.toInt()
private const val COLOR_ROSE    = 0xFFE05C7A.toInt()
private const val COLOR_MUTED   = 0xFF888888.toInt()

fun buildComplianceReport(
    data: ComplianceReportData,
    audience: ComplianceAudience,
    period: ExportPeriod,
    includeAttendance: Boolean,
    includeMeditation: Boolean
): PdfExporter.PdfReport {
    val audienceLabel = when (audience) {
        ComplianceAudience.ALL -> "All members"
        ComplianceAudience.COMPASSION_ONLY -> "Compassion participants"
        ComplianceAudience.NON_COMPASSION -> "Non-Compassion members"
    }
    val includedSections = buildList {
        if (includeAttendance) add("Event Attendance")
        if (includeMeditation) add("Weekly Meditation")
    }.joinToString(", ").ifBlank { "None" }

    val cover = PdfExporter.CoverInfo(
        ownerName = "Royals: The Kingdom Builders",
        ownerSubtitle = "Compliance Report",
        extras = listOf(
            "Audience" to audienceLabel,
            "Period" to period.label(),
            "Sections" to includedSections
        )
    )

    val sections = mutableListOf<PdfExporter.PdfBlock>()

    sections += PdfExporter.PdfTwoColumn(
        heading = "Summary",
        pairs = listOf(
            "Total Members" to data.totalUsers.toString(),
            "Compliant" to data.compliantCount.toString(),
            "Attended ≥1" to data.attendingCount.toString(),
            "Reflected ≥1" to data.reflectingCount.toString()
        )
    )

    val cols = mutableListOf("Name", "Role", "Compassion #")
    val weights = mutableListOf(2.4f, 1.2f, 1.4f)
    if (includeAttendance) {
        cols += "Attended"
        weights += 0.9f
    }
    if (includeMeditation) {
        cols += "Reflections"
        weights += 1.0f
    }
    cols += "Status"
    weights += 1.4f

    val rosterRows = data.rows.map { row -> row.toTableRow(includeAttendance, includeMeditation) }
    sections += PdfExporter.PdfTable(
        heading = "Roster",
        columns = cols,
        rows = rosterRows,
        columnWeights = weights
    )

    if (includeAttendance) {
        sections += PdfExporter.PdfSection(
            heading = "Event Attendance — Per Member",
            rows = data.rows.map { row ->
                PdfExporter.PdfRow(
                    left = row.user.name.ifBlank { row.user.email } +
                        if (row.user.isCompassion) " (Compassion)" else "",
                    right = "${row.attendanceCount} attended · " +
                        "${row.presentCount} present / ${row.lateCount} late"
                )
            }
        )
    }
    if (includeMeditation) {
        sections += PdfExporter.PdfSection(
            heading = "Weekly Meditation — Per Member",
            rows = data.rows.map { row ->
                PdfExporter.PdfRow(
                    left = row.user.name.ifBlank { row.user.email } +
                        if (row.user.isCompassion) " (Compassion)" else "",
                    right = "${row.reflectionCount} reflections"
                )
            }
        )
    }

    return PdfExporter.PdfReport(
        title = "Compliance Report",
        cover = cover,
        sections = sections
    )
}

private fun UserComplianceRow.toTableRow(
    includeAttendance: Boolean,
    includeMeditation: Boolean
): List<PdfExporter.PdfTableCell> {
    val name = user.name.ifBlank { user.email }
    val role = user.role.name.lowercase().replaceFirstChar { it.uppercase() }
    val compassion = if (user.isCompassion)
        (user.compassionNumber ?: "PH867-????")
    else "—"
    val cells = mutableListOf(
        PdfExporter.PdfTableCell(name),
        PdfExporter.PdfTableCell(role, COLOR_MUTED),
        PdfExporter.PdfTableCell(compassion,
            if (user.isCompassion) COLOR_LATE else COLOR_MUTED)
    )
    if (includeAttendance) {
        cells += PdfExporter.PdfTableCell(attendanceCount.toString(),
            if (attendanceCount > 0) COLOR_PRESENT else null)
    }
    if (includeMeditation) {
        cells += PdfExporter.PdfTableCell(reflectionCount.toString(),
            if (reflectionCount > 0) COLOR_PRESENT else null)
    }
    cells += PdfExporter.PdfTableCell(
        if (isCompliant) "Compliant" else "Needs Outreach",
        if (isCompliant) COLOR_PRESENT else COLOR_ROSE
    )
    return cells
}
