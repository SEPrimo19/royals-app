package com.grace.app.presentation.screens.events

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.data.util.PdfExporter
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.Attendee
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EventRosterScreen(
    onBack: () -> Unit,
    // Phase P.2.6 — opens the AddProxyMember form pre-wired with this
    // event's id, so the form's "Save & Mark Attended" path can register
    // the new member AND mark them present in one shot. The eventId is
    // resolved from the screen's nav arg by the parent (NavGraph), so the
    // callback takes no parameters here.
    onAddProxyMember: () -> Unit,
    viewModel: EventRosterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Toast auto-dismiss — same 2.5s window as other screens.
    if (state.toast != null) {
        LaunchedEffect(state.toast) {
            kotlinx.coroutines.delay(2500)
            viewModel.onEvent(EventRosterEvent.DismissToast)
        }
    }

    val notYetCount = state.roster.count { it.status == AttendanceStatus.ABSENT }
    val markedCount = state.roster.size - notYetCount
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportToast by remember { mutableStateOf<String?>(null) }
    if (exportToast != null) {
        LaunchedEffect(exportToast) {
            kotlinx.coroutines.delay(2500)
            exportToast = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Roster", color = GraceCream, fontSize = 22.sp)
                Text(
                    state.event?.title ?: "Loading…",
                    color = GraceCreamDim, fontSize = 12.sp
                )
                state.event?.eventDate?.let { d ->
                    Text(
                        d.format(dateFmt),
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
            }
            // P.2.6 — register a new no-app member mid-meeting + auto-mark
            // them attended. Same styling as MyMembers' + Add chip.
            Text(
                "+ Add",
                color = GraceGold, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        GraceGold.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    )
                    .clickable { onAddProxyMember() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        if (state.toast != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                state.toast!!,
                color = if (state.toast!!.startsWith("✓")) GraceGreen else GraceRose,
                fontSize = 12.sp
            )
        }
        if (exportToast != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                exportToast!!,
                color = if (exportToast!!.startsWith("✓")) GraceGreen else GraceRose,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(12.dp))
        // Summary line — after the event ends, swap "still pending" for
        // "absent" so the leader sees an unambiguous count.
        val pendingLabel = if (state.eventHasEnded) "absent" else "still pending"
        Text(
            "$markedCount marked · $notYetCount $pendingLabel",
            color = GraceCreamDim, fontSize = 11.sp
        )

        Spacer(Modifier.height(10.dp))
        FilterChipsRow(
            current = state.filter,
            counts = state.counts,
            onSelect = {
                viewModel.onEvent(EventRosterEvent.FilterChanged(it))
            }
        )

        // Export filtered roster — only meaningful once the roster has loaded.
        if (!state.isLoading && state.roster.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ExportRosterRow(
                isExporting = isExporting,
                filter = state.filter,
                filteredCount = state.filteredRoster.size,
                onExport = {
                    val ev = state.event ?: return@ExportRosterRow
                    isExporting = true
                    scope.launch {
                        val report = buildRosterReport(
                            event = ev,
                            filter = state.filter,
                            rows = state.filteredRoster,
                            eventHasEnded = state.eventHasEnded
                        )
                        val safeName = ev.title
                            .filter { it.isLetterOrDigit() || it == ' ' }
                            .replace(' ', '_')
                            .ifBlank { "Event" }
                        val uri = withContext(Dispatchers.IO) {
                            PdfExporter.saveToGallery(
                                context, report,
                                "Royals_Roster_${safeName}_${state.filter.label}.pdf"
                            )
                        }
                        exportToast = if (uri != null)
                            "✓ Saved to Documents/Royals."
                        else "⚠ Couldn't save PDF."
                        isExporting = false
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
            state.error != null && state.roster.isEmpty() ->
                Text("⚠ ${state.error}", color = GraceRose)
            state.roster.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "Your cell has no members yet.",
                    color = GraceCreamDim
                )
            }
            state.filteredRoster.isEmpty() -> Box(
                Modifier.fillMaxSize(), Alignment.Center
            ) {
                Text(
                    "No members match the \"${state.filter.label}\" filter.",
                    color = GraceCreamDim
                )
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filteredRoster, key = { it.user.id }) { row ->
                    RosterRow(
                        row = row,
                        isWorking = row.user.id in state.inFlightMemberIds,
                        eventHasEnded = state.eventHasEnded,
                        onMark = { status ->
                            viewModel.onEvent(EventRosterEvent.MarkAttendance(row.user.id, status))
                        },
                        onUndo = {
                            viewModel.onEvent(EventRosterEvent.UndoAttendance(row.user.id))
                        }
                    )
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    current: RosterFilter,
    counts: Map<AttendanceStatus, Int>,
    onSelect: (RosterFilter) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(RosterFilter.entries.toList()) { f ->
            val selected = f == current
            val count = when (f) {
                RosterFilter.ALL -> counts.values.sum()
                RosterFilter.PRESENT -> counts[AttendanceStatus.PRESENT] ?: 0
                RosterFilter.LATE -> counts[AttendanceStatus.LATE] ?: 0
                RosterFilter.EXCUSED -> counts[AttendanceStatus.EXCUSED] ?: 0
                RosterFilter.ABSENT -> counts[AttendanceStatus.ABSENT] ?: 0
            }
            Text(
                "${f.label} · $count",
                color = if (selected) GraceDeepBlue else GraceCream,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        if (selected) GraceGold else GraceCardBg,
                        RoundedCornerShape(50)
                    )
                    .clickable { onSelect(f) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ExportRosterRow(
    isExporting: Boolean,
    filter: RosterFilter,
    filteredCount: Int,
    onExport: () -> Unit
) {
    Text(
        if (isExporting) "Generating PDF…"
        else "📄 Export ${filter.label} ($filteredCount) as PDF",
        color = GraceGold, fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                GraceGold.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = !isExporting && filteredCount > 0) { onExport() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

/**
 * Build a Compassion-style PDF for the event roster, scoped to the active
 * filter. Reuses the same PdfExporter cover + section blocks the My
 * Progress + admin compliance reports use — consistent visual identity
 * across all leader-facing exports.
 */
private fun buildRosterReport(
    event: com.grace.app.domain.model.Event,
    filter: RosterFilter,
    rows: List<Attendee>,
    eventHasEnded: Boolean
): PdfExporter.PdfReport {
    val dateFmt = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy · h:mm a")
    val cover = PdfExporter.CoverInfo(
        ownerName = event.title,
        ownerSubtitle = event.location?.takeIf { it.isNotBlank() },
        extras = buildList {
            add("Event date" to event.eventDate.format(dateFmt))
            event.endDate?.let { add("End date" to it.format(dateFmt)) }
            add("Filter" to filter.label)
            add("Members in this view" to rows.size.toString())
            add(
                "Status" to
                    if (eventHasEnded) "Event has ended" else "Event still open"
            )
        }
    )

    val tableRows = rows.map { a ->
        val statusText = when (a.status) {
            AttendanceStatus.PRESENT -> "Present"
            AttendanceStatus.LATE -> if (a.lateByMinutes > 0)
                "Late (${a.lateByMinutes}m)" else "Late"
            AttendanceStatus.EXCUSED -> "Excused"
            AttendanceStatus.ABSENT -> "Absent"
        }
        val flag = if (a.user.isProxyOnly) " [no app]" else ""
        listOf(
            PdfExporter.PdfTableCell("${a.user.name.ifBlank { a.user.email }}$flag"),
            PdfExporter.PdfTableCell(statusText)
        )
    }
    val table = PdfExporter.PdfTable(
        heading = "Roster (${filter.label})",
        columns = listOf("Member", "Status"),
        rows = tableRows
    )

    return PdfExporter.PdfReport(
        title = "Event Roster",
        cover = cover,
        sections = listOf(table)
    )
}

@Composable
private fun RosterRow(
    row: Attendee,
    isWorking: Boolean,
    // Flips the "Not yet marked" subtitle to "Absent" once the event's
    // check-in window has closed — makes the unmarked rows unambiguous.
    eventHasEnded: Boolean,
    onMark: (AttendanceStatus) -> Unit,
    onUndo: () -> Unit
) {
    val initial = row.user.name.firstOrNull()?.uppercase() ?: "?"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GracePurple.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, color = GraceCream, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            row.user.name.ifBlank { row.user.email },
                            color = GraceCream, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (row.user.isProxyOnly) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "NO APP",
                                color = GraceCreamDim, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        GraceCreamDim.copy(alpha = 0.18f),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    // Status pill OR pending placeholder. Once the event
                    // has ended, "Not yet marked" becomes "Absent" — the
                    // leader didn't get to them and now they're out.
                    if (row.status == AttendanceStatus.ABSENT) {
                        if (eventHasEnded) {
                            StatusPill(AttendanceStatus.ABSENT, 0)
                        } else {
                            Text(
                                "Not yet marked",
                                color = GraceCreamDim, fontSize = 11.sp
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(row.status, row.lateByMinutes)
                            Spacer(Modifier.size(8.dp))
                            // Undo only available for non-absent rows. Stays
                            // enabled even when isWorking — re-tap is harmless.
                            Text(
                                "Undo",
                                color = GraceRose, fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable(enabled = !isWorking) { onUndo() }
                            )
                        }
                    }
                }
                if (isWorking) {
                    CircularProgressIndicator(
                        color = GraceGold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            // Action buttons appear ONLY for not-yet-marked members.
            if (row.status == AttendanceStatus.ABSENT) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        label = "Attended",
                        accent = GraceGreen,
                        enabled = !isWorking,
                        onClick = { onMark(AttendanceStatus.PRESENT) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        label = "Late",
                        accent = GraceOrange,
                        enabled = !isWorking,
                        onClick = { onMark(AttendanceStatus.LATE) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        label = "Excused",
                        accent = GraceGold,
                        enabled = !isWorking,
                        onClick = { onMark(AttendanceStatus.EXCUSED) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: AttendanceStatus, lateBy: Int) {
    val (label, color) = when (status) {
        AttendanceStatus.PRESENT -> "✓ Present" to GraceGreen
        AttendanceStatus.LATE -> ("⏱ Late" + if (lateBy > 0) " · $lateBy min" else "") to GraceOrange
        AttendanceStatus.EXCUSED -> "⛔ Excused" to GraceGold
        AttendanceStatus.ABSENT -> "✗ Absent" to GraceRose
    }
    Text(
        label,
        color = color, fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun ActionButton(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .background(
                accent.copy(alpha = if (enabled) 0.18f else 0.08f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
