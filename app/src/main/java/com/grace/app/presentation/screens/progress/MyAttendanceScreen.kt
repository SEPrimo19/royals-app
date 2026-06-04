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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.grace.app.data.util.PdfExporter
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.AttendedEvent
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MyAttendanceScreen(
    onBack: () -> Unit,
    viewModel: MyAttendanceViewModel = hiltViewModel()
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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("My Attendance", color = GraceCream, fontSize = 24.sp)
                Text(
                    "Events you've checked into.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
            if (state.attended.isNotEmpty()) {
                Text(
                    if (isExporting) "…" else "📄 PDF",
                    color = GraceGold, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            GraceGold.copy(alpha = 0.18f),
                            RoundedCornerShape(50)
                        )
                        .clickable(enabled = !isExporting) {
                            isExporting = true
                            scope.launch {
                                val report = buildAttendanceReport(state)
                                val uri = kotlinx.coroutines.withContext(
                                    kotlinx.coroutines.Dispatchers.IO
                                ) {
                                    PdfExporter.saveToGallery(
                                        context, report,
                                        "Royals_Attendance.pdf"
                                    )
                                }
                                toast = if (uri != null)
                                    "✓ Saved to Documents/Royals."
                                else "⚠ Couldn't save PDF."
                                isExporting = false
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
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

        when {
            state.isLoading && state.attended.isEmpty() ->
                Box(Modifier.fillMaxWidth().height(220.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.error != null && state.attended.isEmpty() ->
                Text("⚠ ${state.error}", color = GraceRose)
            state.attended.isEmpty() ->
                EmptyState()
            else -> {
                StatsRow(
                    total = state.totalAllTime,
                    month = state.thisMonth,
                    year = state.thisYear
                )
                state.mostRecent?.let { recent ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Most recent: ${recent.format(headerFmt())}",
                        color = GraceCreamDim, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                AttendanceList(state.attended)
            }
        }
    }
}

@Composable
private fun StatsRow(total: Int, month: Int, year: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("This month", month.toString(), Modifier.weight(1f))
        StatTile("This year", year.toString(), Modifier.weight(1f))
        StatTile("All time", total.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = GraceGold, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                label.uppercase(), color = GraceCreamDim, fontSize = 9.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AttendanceList(attended: List<AttendedEvent>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(attended, key = { it.event.id + it.attendedAt }) { row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraceCardBg),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val accent = if (row.status == AttendanceStatus.LATE)
                            GraceOrange else GraceGreen
                        val mark = if (row.status == AttendanceStatus.LATE)
                            "⏱" else "✓"
                        Text(mark, color = accent, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold)
                        Text(
                            "  ${row.event.title}",
                            color = GraceCream, fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(row)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "📆 ${row.event.eventDate.format(headerFmt())}",
                        color = GraceGold, fontSize = 11.sp
                    )
                    if (!row.event.location.isNullOrBlank()) {
                        Text(
                            "📍 ${row.event.location}",
                            color = GraceCreamDim, fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Checked in ${row.attendedAt.format(headerFmt())}",
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎟️", fontSize = 32.sp, color = GraceCream)
            Spacer(Modifier.height(8.dp))
            Text(
                "No attendance recorded yet",
                color = GraceCream, fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Scan the QR at your next event to start your record.",
                color = GraceCreamDim, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StatusPill(row: AttendedEvent) {
    val accent = if (row.status == AttendanceStatus.LATE) GraceOrange else GraceGreen
    val label = if (row.status == AttendanceStatus.LATE) {
        if (row.lateByMinutes > 0) "Late · ${row.lateByMinutes} min" else "Late"
    } else "Present"
    Text(
        label,
        color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun headerFmt(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")

private fun buildAttendanceReport(
    state: MyAttendanceUiState
): com.grace.app.data.util.PdfExporter.PdfReport {
    val statsSection = com.grace.app.data.util.PdfExporter.PdfSection(
        heading = "Summary",
        rows = listOf(
            com.grace.app.data.util.PdfExporter.PdfRow(
                "Events this month", state.thisMonth.toString()
            ),
            com.grace.app.data.util.PdfExporter.PdfRow(
                "Events this year", state.thisYear.toString()
            ),
            com.grace.app.data.util.PdfExporter.PdfRow(
                "Events all time", state.totalAllTime.toString()
            )
        )
    )
    val entriesSection = com.grace.app.data.util.PdfExporter.PdfSection(
        heading = "Attendance history",
        rows = state.attended.map { row ->
            val statusBadge = when (row.status) {
                AttendanceStatus.LATE -> if (row.lateByMinutes > 0)
                    "Late · ${row.lateByMinutes}m" else "Late"
                AttendanceStatus.ABSENT -> "Absent"
                AttendanceStatus.EXCUSED -> "Excused"
                AttendanceStatus.PRESENT -> "Present"
            }
            com.grace.app.data.util.PdfExporter.PdfRow(
                left = "• ${row.event.title} · ${row.event.eventDate.format(headerFmt())}",
                right = statusBadge
            )
        }
    )
    return com.grace.app.data.util.PdfExporter.PdfReport(
        title = "Royals — My Attendance",
        subtitle = state.mostRecent?.let { "Most recent check-in: ${it.format(headerFmt())}" },
        sections = listOf(statsSection, entriesSection)
    )
}

@Suppress("unused")
private val unused: LocalDateTime? = null
