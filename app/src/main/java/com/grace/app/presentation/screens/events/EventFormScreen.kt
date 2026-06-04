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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Which picker is currently open. Drives a single shared DatePickerDialog
// / TimePickerDialog so we don't duplicate dialog setup for start vs end.
private enum class PickerTarget { START_DATE, START_TIME, END_DATE, END_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    onDone: () -> Unit,
    viewModel: EventFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<String?>(null) }
    var activePicker by remember { mutableStateOf<PickerTarget?>(null) }

    // Auto-dismiss toasts after 2.5s so they don't linger.
    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                EventFormEffect.Saved -> {
                    toast = "✓ Saved."
                    delay(700)
                    onDone()
                }
                is EventFormEffect.ShowError -> toast = "⚠ ${fx.message}"
            }
        }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }

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
                modifier = Modifier.clickable { onDone() }.padding(end = 12.dp)
            )
            Text(
                if (state.isEditMode) "Edit Event" else "New Event",
                color = GraceCream, fontSize = 24.sp
            )
        }

        if (toast != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                toast!!,
                color = if (toast!!.startsWith("✓")) GraceGold else GraceRose
            )
        }

        Spacer(Modifier.height(16.dp))
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(180.dp), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
        } else {
            SectionHeader("TITLE")
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onEvent(EventFormEvent.TitleChanged(it)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Friday Youth Night", color = GraceCreamDim) }
            )

            Spacer(Modifier.height(14.dp))
            SectionHeader("STARTS")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerPill(
                    label = state.date.format(dateFmt),
                    modifier = Modifier.weight(1f)
                ) { activePicker = PickerTarget.START_DATE }
                PickerPill(
                    label = state.time.format(timeFmt),
                    modifier = Modifier.weight(1f)
                ) { activePicker = PickerTarget.START_TIME }
            }

            Spacer(Modifier.height(14.dp))
            // The "ENDS" row controls when check-in closes. If the user
            // clears it, the legacy "+2h after start" window applies
            // server-side. We surface that hint inline so the behavior
            // isn't a mystery.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ENDS",
                    color = GraceCreamDim, fontSize = 10.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (state.endDate != null) {
                    Text(
                        "Clear",
                        color = GraceRose, fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { viewModel.onEvent(EventFormEvent.ClearEnd) }
                            .padding(start = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (state.endDate != null && state.endTime != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerPill(
                        label = state.endDate!!.format(dateFmt),
                        modifier = Modifier.weight(1f)
                    ) { activePicker = PickerTarget.END_DATE }
                    PickerPill(
                        label = state.endTime!!.format(timeFmt),
                        modifier = Modifier.weight(1f)
                    ) { activePicker = PickerTarget.END_TIME }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Late check-ins between start and end time are recorded " +
                        "as ‘late by N min’. Scans after end time are " +
                        "blocked.",
                    color = GraceCreamDim, fontSize = 11.sp
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerPill(
                        label = "+ Add end time",
                        modifier = Modifier.weight(1f)
                    ) {
                        // Re-add a default end so the pickers reappear. Uses
                        // start+2h, the same default we apply on first open.
                        viewModel.onEvent(
                            EventFormEvent.EndDateChanged(state.date)
                        )
                        viewModel.onEvent(
                            EventFormEvent.EndTimeChanged(state.time.plusHours(2))
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Without an explicit end, check-in stays open for 2 " +
                        "hours after the start time.",
                    color = GraceCreamDim, fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            SectionHeader("LOCATION")
            OutlinedTextField(
                value = state.location,
                onValueChange = { viewModel.onEvent(EventFormEvent.LocationChanged(it)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Main Sanctuary", color = GraceCreamDim) }
            )

            Spacer(Modifier.height(14.dp))
            SectionHeader("DESCRIPTION")
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(EventFormEvent.DescriptionChanged(it)) },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                placeholder = {
                    Text(
                        "What's happening? Who's it for?",
                        color = GraceCreamDim
                    )
                }
            )

            Spacer(Modifier.height(14.dp))
            // Attendance toggle. Off = pure announcement (Sunday Service
            // type events). When off, the QR card disappears from the
            // events list and any check-in attempt is rejected server-side.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraceCardBg, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Track attendance", color = GraceCream, fontSize = 13.sp)
                    Text(
                        if (state.requiresAttendance)
                            "QR code + present/late tracking enabled."
                        else
                            "No QR. Announcement-only event " +
                                "(e.g. Sunday Service reminder).",
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
                Switch(
                    checked = state.requiresAttendance,
                    onCheckedChange = {
                        viewModel.onEvent(
                            EventFormEvent.RequiresAttendanceChanged(it)
                        )
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GraceGold)
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraceCardBg, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recurring event", color = GraceCream, fontSize = 13.sp)
                    Text(
                        "Flag this as a regular event (weekly, monthly). " +
                            "Doesn't auto-create future copies yet.",
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
                Switch(
                    checked = state.isRecurring,
                    onCheckedChange = {
                        viewModel.onEvent(EventFormEvent.RecurringChanged(it))
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GraceGold)
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.onEvent(EventFormEvent.Save) },
                enabled = state.canSave,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) CircularProgressIndicator(
                    color = GraceDeepBlue, modifier = Modifier.size(18.dp)
                ) else Text(
                    if (state.isEditMode) "Save changes" else "Create event",
                    color = GraceDeepBlue, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ---- Shared pickers ------------------------------------------------
    if (activePicker == PickerTarget.START_DATE ||
        activePicker == PickerTarget.END_DATE
    ) {
        val initial = if (activePicker == PickerTarget.END_DATE)
            (state.endDate ?: state.date) else state.date
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
        val target = activePicker
        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onEvent(
                            if (target == PickerTarget.END_DATE)
                                EventFormEvent.EndDateChanged(picked)
                            else EventFormEvent.DateChanged(picked)
                        )
                    }
                    activePicker = null
                }) { Text("OK", color = GraceGold) }
            },
            dismissButton = {
                TextButton(onClick = { activePicker = null }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (activePicker == PickerTarget.START_TIME ||
        activePicker == PickerTarget.END_TIME
    ) {
        val initial = if (activePicker == PickerTarget.END_TIME)
            (state.endTime ?: state.time.plusHours(2)) else state.time
        val timePickerState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = false
        )
        val target = activePicker
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    val picked = LocalTime.of(
                        timePickerState.hour, timePickerState.minute
                    )
                    viewModel.onEvent(
                        if (target == PickerTarget.END_TIME)
                            EventFormEvent.EndTimeChanged(picked)
                        else EventFormEvent.TimeChanged(picked)
                    )
                    activePicker = null
                }) { Text("OK", color = GraceGold) }
            },
            dismissButton = {
                TextButton(onClick = { activePicker = null }) { Text("Cancel") }
            },
            title = { Text("Pick a time") },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label, color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 3.sp, fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun PickerPill(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(GraceCardBg, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(label, color = GraceCream, fontSize = 14.sp)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun keepLocalDateImport(d: LocalDate) = Unit
