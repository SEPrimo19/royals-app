package com.grace.app.presentation.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.data.util.PdfExporter
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.AttendedEvent
import com.grace.app.domain.model.ProgressSnapshot
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MyProgressScreen(
    onBack: () -> Unit,
    onOpenAttendance: () -> Unit,
    viewModel: MyProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var toast by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("My Progress", color = GraceCream, fontSize = 24.sp)
                Text(
                    "Your journey in faith.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }
        if (toast != null) {
            Spacer(Modifier.height(8.dp))
            Text(toast!!,
                color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(16.dp))

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(220.dp), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
        } else {
            StreakCard(state.snapshot.devoStreak)
            Spacer(Modifier.height(14.dp))
            StatGrid(state)
            Spacer(Modifier.height(20.dp))
            AttendanceLink(
                count = state.snapshot.eventsAttended,
                onClick = onOpenAttendance
            )
            Spacer(Modifier.height(20.dp))
            ExportCard(
                state = state,
                isExporting = isExporting,
                onToggleAll = {
                    viewModel.onEvent(MyProgressEvent.IncludeAllChanged(it))
                },
                onToggleAttendance = {
                    viewModel.onEvent(MyProgressEvent.IncludeAttendanceChanged(it))
                },
                onToggleMeditation = {
                    viewModel.onEvent(MyProgressEvent.IncludeMeditationChanged(it))
                },
                onPeriodChanged = {
                    viewModel.onEvent(MyProgressEvent.PeriodChanged(it))
                },
                onExport = {
                    isExporting = true
                    scope.launch {
                        val report = buildProgressReport(state)
                        val uri = withContext(Dispatchers.IO) {
                            PdfExporter.saveToGallery(
                                context, report,
                                "Royals_Progress.pdf"
                            )
                        }
                        toast = if (uri != null)
                            "✓ Saved to Documents/Royals."
                        else "⚠ Couldn't save PDF."
                        isExporting = false
                    }
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StreakCard(streak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔥", fontSize = 44.sp)
            Spacer(Modifier.padding(horizontal = 8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$streak",
                    color = GraceGold, fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (streak == 1) "day streak" else "day streak",
                    color = GraceCreamDim, fontSize = 11.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
            }
            Text(
                if (streak == 0) "Start today 🌱"
                else "Keep going!",
                color = GraceGold, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StatGrid(state: MyProgressUiState) {
    val s = state.snapshot
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                emoji = "📖", value = s.devotionalsCompleted,
                label = "Devotionals", accent = GraceGold,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                emoji = "🙏", value = s.prayersPosted,
                label = "Prayers posted", accent = GraceBlue,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                emoji = "✨", value = s.prayersAnswered,
                label = "Prayers answered", accent = GraceGreen,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                emoji = "💗", value = s.prayersInterceded,
                label = "Prayers lifted", accent = GraceRose,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                emoji = "🌿", value = s.postsShared,
                label = "Posts shared", accent = GracePurple,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                emoji = "🎟️", value = s.eventsAttended,
                label = "Events attended", accent = GraceOrange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    emoji: String,
    value: Int,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "$value", color = accent, fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label.uppercase(), color = GraceCreamDim, fontSize = 9.sp,
                letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

// Date formatters used by the report — module-level so they're built once.
private val reportDateFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy")
private val reportDateTimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")
private val monthHeaderFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM yyyy")
private val timelineDateFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timelineTimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a")

// Status colors picked to match the in-app palette.
private const val COLOR_PRESENT = 0xFF3ECF8E.toInt()  // GraceGreen
private const val COLOR_LATE    = 0xFFF4A261.toInt()  // GraceOrange
private const val COLOR_ABSENT  = 0xFFE05C7A.toInt()  // GraceRose
private const val COLOR_DKGRAY  = 0xFF444444.toInt()  // body default

/**
 * True when the given timestamp falls inside the requested period.
 * AllTime always returns true so the predicate works as a one-liner filter.
 */
private fun ExportPeriod.includes(dt: java.time.LocalDateTime): Boolean = when (this) {
    is ExportPeriod.AllTime -> true
    is ExportPeriod.Month -> YearMonth.from(dt.toLocalDate()) == yearMonth
    is ExportPeriod.WholeYear -> dt.year == year
}

/**
 * Build the progress / compliance PDF for the given state. `generatedBy`
 * is rendered as a small "Generated by {name}" line in the cover extras —
 * null for self-reports (the owner generated it), set to the leader's
 * display name for proxy reports built on behalf of a no-app member.
 * The leader's name is for audit + provenance; the rest of the cover
 * still shows the OWNER (the report subject), per pastoral spec.
 *
 * Visibility is `internal` so MemberReportScreen (Phase P.4) can reuse
 * this builder without duplicating ~200 lines of layout logic.
 */
internal fun buildProgressReport(
    state: MyProgressUiState,
    generatedBy: String? = null
): PdfExporter.PdfReport {
    val s = state.snapshot
    val sections = mutableListOf<PdfExporter.PdfBlock>()
    val me = state.me

    // Cover page identity. Owner = the user. ownerSubtitle combines role +
    // Compassion ID (when applicable) so the Compassion director can verify
    // the participant in one glance. extras line shows which filter sections
    // are in the export, mirroring the screenshot's "Filters: …" line.
    val ownerName = me?.name?.takeIf { it.isNotBlank() }
        ?: me?.email
        ?: "GRACE Member"
    val ownerSubtitleParts = mutableListOf<String>()
    me?.let { user ->
        ownerSubtitleParts += "Role: " + user.role.name.lowercase()
            .replaceFirstChar { it.uppercase() }
        if (user.isCompassion && !user.compassionNumber.isNullOrBlank()) {
            ownerSubtitleParts += "Compassion ID: ${user.compassionNumber}"
        }
    }
    val ownerSubtitle = ownerSubtitleParts
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
    val includedFilters = buildList {
        if (state.includeAll) add("Personal Stats")
        if (state.includeAttendance) add("Event Attendance")
        if (state.includeMeditation) add("Weekly Meditation")
    }.joinToString(", ").ifBlank { "None" }
    val extras = buildList {
        add("Period" to state.period.label())
        add("Filters" to includedFilters)
        // Audit attribution when a leader generates on behalf of a member.
        if (!generatedBy.isNullOrBlank()) {
            add("Generated by" to generatedBy)
        }
    }
    val cover = PdfExporter.CoverInfo(
        ownerName = ownerName,
        ownerSubtitle = ownerSubtitle,
        // Period is surfaced on the cover so the recipient knows exactly
        // what window the data covers — important for Compassion submissions
        // where the project director files monthly reports separately.
        extras = extras
    )

    // "All" includes the personal-stats sections (devo/prayer/community).
    // Unchecked = a tight Compassion-compliance report with only the
    // attendance + reflection items.
    if (state.includeAll) {
        sections += PdfExporter.PdfSection(
            heading = "Devotional",
            rows = listOf(
                PdfExporter.PdfRow("Current streak", "${s.devoStreak} days"),
                PdfExporter.PdfRow("Devotionals completed",
                    s.devotionalsCompleted.toString())
            )
        )
        sections += PdfExporter.PdfSection(
            heading = "Prayer Wall",
            rows = listOf(
                PdfExporter.PdfRow("Prayers I posted", s.prayersPosted.toString()),
                PdfExporter.PdfRow("Prayers answered", s.prayersAnswered.toString()),
                PdfExporter.PdfRow("Times I prayed for others",
                    s.prayersInterceded.toString())
            )
        )
        sections += PdfExporter.PdfSection(
            heading = "Community",
            rows = listOf(
                PdfExporter.PdfRow("Posts shared", s.postsShared.toString())
            )
        )
    }

    if (state.includeAttendance) {
        // ---- Attendance Summary (2-column) -----------------------------------
        // Period filter applied here so every downstream metric (summary,
        // risk, monthly breakdown, timeline) uses the same slice of data.
        val attended = state.attendedEvents
            .filter { state.period.includes(it.attendedAt) }
        val present = attended.count { it.status == AttendanceStatus.PRESENT }
        val late = attended.count { it.status == AttendanceStatus.LATE }
        // Excused / Absent: we don't store absent rows server-side (per
        // schema, "absent is the creator-roster derivation, not a row").
        // Mark as N/A rather than guessing.
        val total = attended.size
        val attendanceRate = if (total == 0) 0f
            else (present + late) * 100f / total
        sections += PdfExporter.PdfTwoColumn(
            heading = "Attendance Summary",
            pairs = listOf(
                "Total Events" to total.toString(),
                "Present" to present.toString(),
                "Attendance Rate" to "%.1f%%".format(attendanceRate),
                "Late" to late.toString(),
                "Excused" to "—",
                "Absent" to "—"
            )
        )

        // ---- Risk & Monitoring Insights (2-column) ---------------------------
        // Computed from what we actually have. Compassion templates expect
        // these metrics; gaps are surfaced as "—" rather than fabricated.
        val sorted = attended.sortedByDescending { it.attendedAt }
        val lastAttended = sorted.firstOrNull()?.attendedAt
        val daysSinceLast = lastAttended?.let {
            ChronoUnit.DAYS.between(it.toLocalDate(), LocalDate.now())
        }
        val onTimeRate = if (present + late == 0) 0f
            else present * 100f / (present + late)
        val currentStreak = sorted.takeWhile {
            it.status == AttendanceStatus.PRESENT
        }.size
        val riskLevel = when {
            daysSinceLast == null -> "Unknown"
            daysSinceLast >= 30 -> "High"
            daysSinceLast >= 14 -> "Medium"
            else -> "Low"
        }
        val riskReason = when (riskLevel) {
            "High" -> "30+ days since last attendance"
            "Medium" -> "14+ days since last attendance"
            "Low" -> "Active in last 2 weeks"
            else -> "No attendance on record"
        }
        val priority = when (riskLevel) {
            "High" -> "P1"
            "Medium" -> "P2"
            "Low" -> "P3"
            else -> "—"
        }
        val recommendedAction = when (riskLevel) {
            "High" -> "Immediate outreach within 24 hours; contact participant and guardian."
            "Medium" -> "Personal follow-up this week."
            "Low" -> "Continue regular encouragement."
            else -> "Reach out to confirm participation."
        }
        sections += PdfExporter.PdfTwoColumn(
            heading = "Risk & Monitoring Insights",
            pairs = listOf(
                "Risk Level" to riskLevel,
                "On-Time Rate" to "%.1f%%".format(onTimeRate),
                "Risk Reason" to riskReason,
                "Latest Status" to (sorted.firstOrNull()?.status?.name
                    ?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "—"),
                "Last Attended" to (lastAttended?.format(timelineDateFmt) ?: "—"),
                "Days Since Last" to (daysSinceLast?.toString() ?: "—"),
                "Priority" to priority,
                "Current Streak" to currentStreak.toString()
            )
        )
        // Recommended Action stretches the full width — render as plain rows.
        sections += PdfExporter.PdfSection(
            heading = "Recommended Action",
            rows = listOf(PdfExporter.PdfRow(recommendedAction))
        )

        // ---- Monthly Breakdown (table) ---------------------------------------
        // Month range adapts to the selected period:
        //   - AllTime    → last 6 months (current behavior)
        //   - Month X    → just X
        //   - WholeYear Y → all 12 months of Y
        val today = LocalDate.now()
        val monthsToShow: List<YearMonth> = when (val p = state.period) {
            is ExportPeriod.AllTime ->
                (0L..5L).map { YearMonth.from(today.minusMonths(it)) }.reversed()
            is ExportPeriod.Month -> listOf(p.yearMonth)
            is ExportPeriod.WholeYear ->
                (1..12).map { YearMonth.of(p.year, it) }
        }
        val monthlyRows = monthsToShow.map { ym ->
            val monthAttended = attended.filter {
                YearMonth.from(it.attendedAt.toLocalDate()) == ym
            }
            val mPresent = monthAttended.count { it.status == AttendanceStatus.PRESENT }
            val mLate = monthAttended.count { it.status == AttendanceStatus.LATE }
            listOf(
                PdfExporter.PdfTableCell(ym.format(monthHeaderFmt)),
                PdfExporter.PdfTableCell(mPresent.toString(),
                    if (mPresent > 0) COLOR_PRESENT else null),
                PdfExporter.PdfTableCell(mLate.toString(),
                    if (mLate > 0) COLOR_LATE else null)
            )
        }
        sections += PdfExporter.PdfTable(
            heading = "Monthly Breakdown",
            columns = listOf("Month", "Present", "Late"),
            rows = monthlyRows,
            columnWeights = listOf(2f, 1f, 1f)
        )

        // ---- Recent Attendance Timeline (table) ------------------------------
        // Most recent 12 — keeps the report at a reasonable length while
        // still showing the trend. Older history is in the user's data; we
        // export the actionable snapshot.
        val timelineRows = sorted.take(12).map { ae ->
            val statusColor = when (ae.status) {
                AttendanceStatus.PRESENT -> COLOR_PRESENT
                AttendanceStatus.LATE -> COLOR_LATE
                AttendanceStatus.EXCUSED -> COLOR_PRESENT  // "accounted for"
                AttendanceStatus.ABSENT -> COLOR_ABSENT
            }
            val statusText = when (ae.status) {
                AttendanceStatus.PRESENT -> "Present"
                AttendanceStatus.LATE -> if (ae.lateByMinutes > 0)
                    "Late · ${ae.lateByMinutes}m" else "Late"
                AttendanceStatus.EXCUSED -> "Excused"
                AttendanceStatus.ABSENT -> "Absent"
            }
            listOf(
                PdfExporter.PdfTableCell(ae.attendedAt.format(timelineDateFmt)),
                PdfExporter.PdfTableCell(ae.event.title),
                PdfExporter.PdfTableCell(statusText, statusColor),
                PdfExporter.PdfTableCell(ae.attendedAt.format(timelineTimeFmt))
            )
        }
        sections += PdfExporter.PdfTable(
            heading = "Recent Attendance Timeline",
            columns = listOf("Date", "Event", "Status", "Time In"),
            rows = if (timelineRows.isEmpty()) listOf(listOf(
                PdfExporter.PdfTableCell("—"),
                PdfExporter.PdfTableCell("No attendance recorded yet."),
                PdfExporter.PdfTableCell("—"),
                PdfExporter.PdfTableCell("—")
            )) else timelineRows,
            columnWeights = listOf(1.4f, 3.2f, 1.6f, 1.2f)
        )
    }

    if (state.includeMeditation) {
        // Same period filter applied to reflections — submittedAt is the
        // anchor (when the user wrote it), not the meditation's week. That
        // matches what a Compassion director cares about: when DID the
        // submission happen?
        val periodReflections = state.reflections.filter {
            state.period.includes(it.submission.submittedAt)
        }
        val rows = mutableListOf(
            PdfExporter.PdfRow("Reflections submitted",
                periodReflections.size.toString())
        )
        periodReflections.forEach { item ->
            val title = item.meditation?.title ?: "Meditation"
            val ref = item.meditation?.scriptureRef ?: "—"
            rows += PdfExporter.PdfRow(
                left = "Week ${item.meditation?.weekNumber ?: "?"} — $title ($ref)",
                right = item.submission.submittedAt.format(reportDateFmt)
            )
            // Reflection text on its own row, left side, no right — the
            // PdfExporter wraps long text. Indented by emoji-arrow for hierarchy.
            rows += PdfExporter.PdfRow(
                left = "  ↳ ${item.submission.reflectionText}",
                right = ""
            )
        }
        if (periodReflections.isEmpty()) {
            rows += PdfExporter.PdfRow("", "No reflections submitted in this period.")
        }
        sections += PdfExporter.PdfSection(
            heading = "Weekly Meditation Reflections",
            rows = rows
        )
    }

    // If user unchecked everything, we still produce a stub so the PDF
    // isn't blank. Honest fallback rather than silently exporting nothing.
    if (sections.isEmpty()) {
        sections += PdfExporter.PdfSection(
            heading = "No sections selected",
            rows = listOf(
                PdfExporter.PdfRow(
                    "",
                    "Check at least one box before exporting."
                )
            )
        )
    }

    return PdfExporter.PdfReport(
        title = "Participant Progress Report",
        // Subtitle is rendered on the simple layout only; when cover is set
        // the cover page handles all top-of-document info.
        subtitle = null,
        cover = cover,
        sections = sections
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportCard(
    state: MyProgressUiState,
    isExporting: Boolean,
    onToggleAll: (Boolean) -> Unit,
    onToggleAttendance: (Boolean) -> Unit,
    onToggleMeditation: (Boolean) -> Unit,
    onPeriodChanged: (ExportPeriod) -> Unit,
    onExport: () -> Unit
) {
    val canExport = !isExporting &&
        (state.includeAll || state.includeAttendance || state.includeMeditation)
    val periodOptions = remember { buildPeriodOptions() }
    var periodMenuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "📄  EXPORT PDF",
                color = GraceGold, fontSize = 10.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a period and which sections to include.",
                color = GraceCreamDim, fontSize = 11.sp
            )

            // ---- Period dropdown -----------------------------------------
            Spacer(Modifier.height(12.dp))
            Text(
                "PERIOD",
                color = GraceCreamDim, fontSize = 9.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = periodMenuOpen,
                onExpandedChange = { periodMenuOpen = !periodMenuOpen }
            ) {
                OutlinedTextField(
                    value = state.period.label(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = periodMenuOpen
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GraceCream,
                        unfocusedTextColor = GraceCream,
                        focusedContainerColor = GraceDeepBlue,
                        unfocusedContainerColor = GraceDeepBlue,
                        focusedBorderColor = GraceGold,
                        unfocusedBorderColor = GraceCreamDim.copy(alpha = 0.4f),
                        cursorColor = GraceGold
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = periodMenuOpen,
                    onDismissRequest = { periodMenuOpen = false }
                ) {
                    periodOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            onClick = {
                                onPeriodChanged(option)
                                periodMenuOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            FilterRow("All", state.includeAll, onToggleAll,
                "Personal stats (devotional, prayer, community).")
            FilterRow("Event Attendance", state.includeAttendance, onToggleAttendance,
                "Per-event list with status.")
            FilterRow("Weekly Meditation", state.includeMeditation, onToggleMeditation,
                "Your reflections — required for Compassion compliance.")
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (canExport) GraceGold else GraceCardBg,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = canExport) { onExport() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        color = GraceDeepBlue, strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        "Export PDF",
                        color = if (canExport) GraceDeepBlue else GraceCreamDim,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    helper: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(
                checkedColor = GraceGold,
                uncheckedColor = GraceCreamDim,
                checkmarkColor = GraceDeepBlue
            )
        )
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = GraceCream, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold)
            Text(helper, color = GraceCreamDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AttendanceLink(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎟️", fontSize = 22.sp)
            Spacer(Modifier.padding(horizontal = 6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("My Attendance", color = GraceCream, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    "$count event${if (count == 1) "" else "s"} on record",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
            Text("→", color = GraceGold, fontSize = 22.sp)
        }
    }
}
