package com.grace.app.presentation.screens.leader

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.data.util.PdfExporter
import com.grace.app.presentation.screens.progress.ExportPeriod
import com.grace.app.presentation.screens.progress.buildPeriodOptions
import com.grace.app.presentation.screens.progress.buildProgressReport
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MemberReportScreen(
    onBack: () -> Unit,
    viewModel: MemberReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(3000)
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
                Text("Compliance Report", color = GraceCream, fontSize = 22.sp)
                Text(
                    "For ${state.memberName}",
                    color = GraceCreamDim, fontSize = 12.sp
                )
                val member = state.progressState.me
                if (member?.isCompassion == true &&
                    !member.compassionNumber.isNullOrBlank()) {
                    Text(
                        member.compassionNumber!!,
                        color = GraceGold, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(220.dp), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
            return@Column
        }

        if (toast != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                toast!!,
                color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        // Summary of what data the leader is about to export — gives them
        // a sanity check before tapping Generate.
        SummaryCard(
            attendanceCount = state.progressState.attendedEvents.size,
            reflectionCount = state.progressState.reflections.size
        )

        Spacer(Modifier.height(16.dp))
        // Period dropdown.
        PeriodPicker(
            current = state.progressState.period,
            onSelect = { viewModel.onEvent(MemberReportEvent.PeriodChanged(it)) }
        )

        Spacer(Modifier.height(16.dp))
        // Filter checkboxes — same flavor as MyProgress.
        Text("Include in report", color = GraceCreamDim, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold)
        FilterToggle(
            label = "Personal stats (devo / prayer / community)",
            checked = state.progressState.includeAll,
            onToggle = { viewModel.onEvent(MemberReportEvent.IncludeAllChanged(it)) }
        )
        FilterToggle(
            label = "Event attendance",
            checked = state.progressState.includeAttendance,
            onToggle = {
                viewModel.onEvent(MemberReportEvent.IncludeAttendanceChanged(it))
            }
        )
        FilterToggle(
            label = "Weekly meditation reflections",
            checked = state.progressState.includeMeditation,
            onToggle = {
                viewModel.onEvent(MemberReportEvent.IncludeMeditationChanged(it))
            }
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    val report = buildProgressReport(
                        state.progressState,
                        generatedBy = state.generatedByLeaderName
                            .takeIf { it.isNotBlank() }
                    )
                    val safeName = state.memberName
                        .filter { it.isLetterOrDigit() || it == ' ' }
                        .replace(' ', '_')
                        .ifBlank { "Member" }
                    val uri = withContext(Dispatchers.IO) {
                        PdfExporter.saveToGallery(
                            context, report,
                            "Royals_${safeName}_Report.pdf"
                        )
                    }
                    toast = if (uri != null)
                        "✓ Saved to Documents/Royals."
                    else "⚠ Couldn't save PDF."
                    isExporting = false
                }
            },
            enabled = !isExporting,
            colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (isExporting) "Generating…" else "📄 Generate PDF",
                color = GraceDeepBlue, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Saved to Documents/Royals. Share to the project director via Messenger, " +
                "email, or print. \"Generated by you\" appears on the cover for audit.",
            color = GraceCreamDim, fontSize = 11.sp
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SummaryCard(attendanceCount: Int, reflectionCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            SummaryStat(
                label = "Attendance",
                value = attendanceCount.toString(),
                modifier = Modifier.weight(1f)
            )
            SummaryStat(
                label = "Reflections",
                value = reflectionCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, color = GraceGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = GraceCreamDim, fontSize = 11.sp)
    }
}

@Composable
private fun PeriodPicker(
    current: ExportPeriod,
    onSelect: (ExportPeriod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { buildPeriodOptions() }
    Text("Period", color = GraceCreamDim, fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = CardDefaults.cardColors(containerColor = GraceCardBg),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(current.label(), color = GraceCream,
                    modifier = Modifier.weight(1f))
                Text("▾", color = GraceGold)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label()) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterToggle(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(checkedColor = GraceGold)
        )
        Text(label, color = GraceCream, fontSize = 13.sp)
    }
}
