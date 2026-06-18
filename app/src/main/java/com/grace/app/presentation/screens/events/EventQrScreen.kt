package com.grace.app.presentation.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.data.util.PdfExporter
import com.grace.app.data.util.QrSaver
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.Attendee
import com.grace.app.domain.model.Event
import com.grace.app.presentation.components.QrCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GraceRose
import java.time.format.DateTimeFormatter

@Composable
fun EventQrScreen(
    onBack: () -> Unit,
    viewModel: EventQrViewModel = hiltViewModel()
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
    LaunchedEffect(Unit) { viewModel.refresh() }

    val dateFmt = remember0()

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
            Text("Attendance QR", color = GraceCream, fontSize = 24.sp)
        }

        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            Text("⚠ ${state.error}", color = GraceRose)
        }

        Spacer(Modifier.height(16.dp))
        if (state.isLoading && state.event == null) {
            EventQrLoadingSkeleton()
        } else state.event?.let { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraceCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(event.title, color = GraceCream, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "📆 ${event.eventDate.format(dateFmt)}",
                        color = GraceGold, fontSize = 12.sp
                    )
                    if (!event.location.isNullOrBlank()) {
                        Text("📍 ${event.location}",
                            color = GraceCreamDim, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            state.qrPayload?.let { payload ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    QrCode(
                        content = payload,
                        modifier = Modifier.fillMaxSize(),
                        foreground = Color.Black,
                        background = Color.White
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Members open their phone camera and point it at this QR " +
                    "code to check in. Check-in opens 1 hour before the " +
                    "event and closes 2 hours after.",
                color = GraceCreamDim, fontSize = 12.sp
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.qrPayload?.let { payload ->
                    val title = state.event?.title ?: "Event"
                    ActionPill(
                        label = if (isExporting) "…Working" else "💾 Save to gallery",
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isExporting) return@ActionPill
                        isExporting = true
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                QrSaver.saveToGallery(context, payload, title)
                            }
                            toast = if (uri != null) "✓ Saved to Pictures/Royals."
                                else "⚠ Couldn't save. Try Share instead."
                            isExporting = false
                        }
                    }
                    ActionPill(
                        label = if (isExporting) "…Working" else "📤 Share",
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isExporting) return@ActionPill
                        isExporting = true
                        scope.launch {
                            val intent = withContext(Dispatchers.IO) {
                                QrSaver.shareIntent(context, payload, title)
                            }
                            if (intent != null) {
                                runCatching { context.startActivity(intent) }
                            } else {
                                toast = "⚠ Couldn't open share sheet."
                            }
                            isExporting = false
                        }
                    }
                }
            }

            if (!state.isLoadingAttendees) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill(
                        label = if (isExporting) "…Working" else "📋 Roster PDF",
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isExporting) return@ActionPill
                        val ev = state.event ?: return@ActionPill
                        isExporting = true
                        scope.launch {
                            val report = buildRosterReport(ev, state.attendees)
                            val uri = withContext(Dispatchers.IO) {
                                PdfExporter.saveToGallery(
                                    context, report,
                                    "Royals_Roster_${ev.title}.pdf"
                                )
                            }
                            toast = if (uri != null)
                                "✓ Saved to Documents/Royals."
                            else "⚠ Couldn't save PDF."
                            isExporting = false
                        }
                    }
                    ActionPill(
                        label = if (isExporting) "…Working" else "📤 Share PDF",
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isExporting) return@ActionPill
                        val ev = state.event ?: return@ActionPill
                        isExporting = true
                        scope.launch {
                            val report = buildRosterReport(ev, state.attendees)
                            val intent = withContext(Dispatchers.IO) {
                                PdfExporter.shareIntent(
                                    context, report,
                                    "Royals_Roster_${ev.title}.pdf"
                                )
                            }
                            if (intent != null) {
                                runCatching { context.startActivity(intent) }
                            } else {
                                toast = "⚠ Couldn't open share sheet."
                            }
                            isExporting = false
                        }
                    }
                }
            }
            if (toast != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    toast!!,
                    color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(20.dp))
            val presentCount = state.attendees.count {
                it.status == AttendanceStatus.PRESENT
            }
            val lateCount = state.attendees.count {
                it.status == AttendanceStatus.LATE
            }
            val absentCount = state.attendees.count {
                it.status == AttendanceStatus.ABSENT
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ROSTER", color = GraceCreamDim, fontSize = 10.sp,
                        letterSpacing = 3.sp, fontWeight = FontWeight.Bold
                    )
                    if (state.isLoadingAttendees) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(
                            color = GraceGold,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "✓ $presentCount",
                        color = GraceGreen, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (lateCount > 0) Text(
                        "⏱ $lateCount",
                        color = GraceOrange, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (absentCount > 0) Text(
                        "✗ $absentCount",
                        color = GraceRose, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (state.attendees.isEmpty()) {
                Text("No one yet — waiting on the first scan.",
                    color = GraceCreamDim, fontSize = 12.sp)
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.attendees.take(8).forEach { row ->
                        AttendeeRow(row)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AttendeeRow(row: Attendee) {
    val accent = when (row.status) {
        AttendanceStatus.PRESENT -> GraceGreen
        AttendanceStatus.LATE -> GraceOrange
        AttendanceStatus.EXCUSED -> GraceGold
        AttendanceStatus.ABSENT -> GraceRose
    }
    val mark = when (row.status) {
        AttendanceStatus.PRESENT -> "✓"
        AttendanceStatus.LATE -> "⏱"
        AttendanceStatus.EXCUSED -> "⛔"
        AttendanceStatus.ABSENT -> "✗"
    }
    val pillLabel = when (row.status) {
        AttendanceStatus.PRESENT -> "Present"
        AttendanceStatus.LATE -> if (row.lateByMinutes > 0)
            "Late · ${row.lateByMinutes} min" else "Late"
        AttendanceStatus.EXCUSED -> "Excused"
        AttendanceStatus.ABSENT -> "Absent"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(mark, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            "  ${row.user.name.ifBlank { row.user.email }}",
            color = if (row.status == AttendanceStatus.ABSENT)
                GraceCreamDim else GraceCream,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(
            pillLabel,
            color = accent, fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(
                    accent.copy(alpha = 0.18f),
                    androidx.compose.foundation.shape.RoundedCornerShape(50)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EventQrLoadingSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GraceCream.copy(alpha = 0.08f))
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GraceGold.copy(alpha = 0.18f))
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(GraceCream.copy(alpha = 0.04f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = GraceGold, strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ActionPill(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(GraceCardBg)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = GraceCream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun remember0(): DateTimeFormatter =
    androidx.compose.runtime.remember {
        DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
    }

private fun buildRosterReport(
    event: Event,
    attendees: List<Attendee>
): PdfExporter.PdfReport {
    val fmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy · h:mm a")
    val present = attendees.filter { it.status == AttendanceStatus.PRESENT }
    val late = attendees.filter { it.status == AttendanceStatus.LATE }
    val absent = attendees.filter { it.status == AttendanceStatus.ABSENT }

    val summary = PdfExporter.PdfSection(
        heading = "Summary",
        rows = listOfNotNull(
            PdfExporter.PdfRow("Event", event.title),
            PdfExporter.PdfRow("When", event.eventDate.format(fmt)),
            event.location?.takeIf { it.isNotBlank() }?.let {
                PdfExporter.PdfRow("Where", it)
            },
            PdfExporter.PdfRow("Present", present.size.toString()),
            PdfExporter.PdfRow("Late", late.size.toString()),
            PdfExporter.PdfRow("Absent", absent.size.toString())
        )
    )

    fun toRows(list: List<Attendee>) = list.map { a ->
        val name = a.user.name.ifBlank { a.user.email }
        val tag = when (a.status) {
            AttendanceStatus.LATE -> if (a.lateByMinutes > 0)
                "Late · ${a.lateByMinutes}m" else "Late"
            AttendanceStatus.ABSENT -> "Absent"
            AttendanceStatus.EXCUSED -> "Excused"
            AttendanceStatus.PRESENT -> "Present"
        }
        PdfExporter.PdfRow(left = "• $name", right = tag)
    }

    val sections = mutableListOf(summary)
    if (present.isNotEmpty()) sections += PdfExporter.PdfSection(
        heading = "Present (${present.size})", rows = toRows(present)
    )
    if (late.isNotEmpty()) sections += PdfExporter.PdfSection(
        heading = "Late (${late.size})", rows = toRows(late)
    )
    if (absent.isNotEmpty()) sections += PdfExporter.PdfSection(
        heading = "Absent (${absent.size})", rows = toRows(absent)
    )

    return PdfExporter.PdfReport(
        title = "Royals — Attendance Roster",
        subtitle = event.title,
        sections = sections
    )
}
