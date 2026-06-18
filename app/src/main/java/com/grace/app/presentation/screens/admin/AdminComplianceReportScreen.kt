package com.grace.app.presentation.screens.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.data.util.PdfExporter
import com.grace.app.domain.usecase.admin.ComplianceAudience
import com.grace.app.domain.usecase.admin.ComplianceReportData
import com.grace.app.domain.usecase.admin.UserComplianceRow
import com.grace.app.presentation.screens.progress.ExportPeriod
import com.grace.app.presentation.screens.progress.buildPeriodOptions
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplianceReportScreen(
    onBack: () -> Unit,
    viewModel: AdminComplianceReportViewModel = hiltViewModel()
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

    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                is AdminComplianceEffect.ShowError -> toast = "⚠ ${fx.message}"
            }
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
            Column {
                Text("Compliance Report", color = GraceCream, fontSize = 24.sp)
                Text(
                    "Generate the monthly report for the Compassion project director.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }
        if (toast != null) {
            Spacer(Modifier.height(10.dp))
            Text(toast!!,
                color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose,
                fontSize = 12.sp
            )
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ${state.error}", color = GraceRose, fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraceCardBg),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SmallLabel("AUDIENCE")
                AudienceDropdown(
                    selected = state.audience,
                    onSelect = {
                        viewModel.onEvent(AdminComplianceEvent.AudienceChanged(it))
                    }
                )

                Spacer(Modifier.height(14.dp))
                SmallLabel("PERIOD")
                PeriodDropdown(
                    selected = state.period,
                    onSelect = {
                        viewModel.onEvent(AdminComplianceEvent.PeriodChanged(it))
                    }
                )

                Spacer(Modifier.height(14.dp))
                FilterCheckRow(
                    label = "Include Event Attendance",
                    helper = "Per-event participation list per member.",
                    checked = state.includeAttendance,
                    onChange = {
                        viewModel.onEvent(
                            AdminComplianceEvent.IncludeAttendanceChanged(it)
                        )
                    }
                )
                FilterCheckRow(
                    label = "Include Weekly Meditation",
                    helper = "Reflection count per member.",
                    checked = state.includeMeditation,
                    onChange = {
                        viewModel.onEvent(
                            AdminComplianceEvent.IncludeMeditationChanged(it)
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
        } else {
            state.report?.let { SummaryCard(it) }
        }

        state.report?.let { report ->
            Spacer(Modifier.height(12.dp))
            Text(
                "ROSTER PREVIEW (${report.rows.size})",
                color = GraceGold, fontSize = 10.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            report.rows.take(10).forEach { row ->
                RosterPreviewRow(row)
                Spacer(Modifier.height(6.dp))
            }
            if (report.rows.size > 10) {
                Text(
                    "+ ${report.rows.size - 10} more in the exported PDF",
                    color = GraceCreamDim, fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        val canExport = !isExporting && state.report != null &&
            (state.includeAttendance || state.includeMeditation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (canExport) GraceGold else GraceCardBg,
                    RoundedCornerShape(12.dp)
                )
                .clickable(enabled = canExport) {
                    val report = state.report ?: return@clickable
                    isExporting = true
                    scope.launch {
                        val pdfReport = buildComplianceReport(
                            data = report,
                            audience = state.audience,
                            period = state.period,
                            includeAttendance = state.includeAttendance,
                            includeMeditation = state.includeMeditation
                        )
                        val uri = withContext(Dispatchers.IO) {
                            PdfExporter.saveToGallery(
                                context, pdfReport,
                                "Royals_Compliance_${state.period.label()
                                    .replace(" ", "_")}.pdf"
                            )
                        }
                        toast = if (uri != null)
                            "✓ Saved to Documents/Royals."
                        else "⚠ Couldn't save PDF."
                        isExporting = false
                    }
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    color = GraceDeepBlue, strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    "Export Compliance PDF",
                    color = if (canExport) GraceDeepBlue else GraceCreamDim,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SmallLabel(text: String) {
    Text(
        text, color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudienceDropdown(
    selected: ComplianceAudience,
    onSelect: (ComplianceAudience) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val label = when (selected) {
        ComplianceAudience.ALL -> "All members"
        ComplianceAudience.COMPASSION_ONLY -> "Compassion participants only"
        ComplianceAudience.NON_COMPASSION -> "Non-Compassion only"
    }
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = !open }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = open)
            },
            colors = dropdownColors(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }
        ) {
            DropdownMenuItem(
                text = { Text("All members") },
                onClick = { onSelect(ComplianceAudience.ALL); open = false }
            )
            DropdownMenuItem(
                text = { Text("Compassion participants only") },
                onClick = {
                    onSelect(ComplianceAudience.COMPASSION_ONLY); open = false
                }
            )
            DropdownMenuItem(
                text = { Text("Non-Compassion only") },
                onClick = { onSelect(ComplianceAudience.NON_COMPASSION); open = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDropdown(
    selected: ExportPeriod,
    onSelect: (ExportPeriod) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val options = remember { buildPeriodOptions() }
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = !open }
    ) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = open)
            },
            colors = dropdownColors(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label()) },
                    onClick = { onSelect(opt); open = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dropdownColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GraceCream,
    unfocusedTextColor = GraceCream,
    focusedContainerColor = GraceDeepBlue,
    unfocusedContainerColor = GraceDeepBlue,
    focusedBorderColor = GraceGold,
    unfocusedBorderColor = GraceCreamDim.copy(alpha = 0.4f),
    cursorColor = GraceGold
)

@Composable
private fun FilterCheckRow(
    label: String,
    helper: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
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
private fun SummaryCard(report: ComplianceReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SmallLabel("SUMMARY")
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                StatCol("Total", report.totalUsers.toString())
                StatCol("Compliant", report.compliantCount.toString(),
                    accent = GraceGreen)
                StatCol("Attended", report.attendingCount.toString())
                StatCol("Reflected", report.reflectingCount.toString())
            }
        }
    }
}

@Composable
private fun StatCol(label: String, value: String, accent: androidx.compose.ui.graphics.Color = GraceCream) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = GraceCreamDim, fontSize = 10.sp,
            letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RosterPreviewRow(row: UserComplianceRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GraceCardBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.user.name.ifBlank { row.user.email },
                    color = GraceCream, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
                if (row.user.isCompassion) {
                    Spacer(Modifier.size(6.dp))
                    Text("CP", color = GraceGold, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(GraceGold.copy(alpha = 0.18f),
                                RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(
                "📅 ${row.attendanceCount}  ·  📖 ${row.reflectionCount}",
                color = GraceCreamDim, fontSize = 11.sp
            )
        }
        Text(
            if (row.isCompliant) "Compliant" else "Needs Outreach",
            color = if (row.isCompliant) GraceGreen else GraceRose,
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(
                    (if (row.isCompliant) GraceGreen else GraceRose)
                        .copy(alpha = 0.18f),
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
