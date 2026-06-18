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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.Event
import com.grace.app.domain.model.RsvpStatus
import com.grace.app.presentation.components.GracePullToRefresh
import com.grace.app.presentation.components.QrScanResult
import com.grace.app.presentation.components.rememberQrScanner
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")

@Composable
fun EventsScreen(
    onBack: () -> Unit,
    onShowQr: (String) -> Unit = {},
    onShowRoster: (String) -> Unit = {},
    onCreateEvent: () -> Unit = {},
    onEditEvent: (String) -> Unit = {},
    onScannedEvent: (String) -> Unit = {},
    onEmailEvent: (String, String) -> Unit = { _, _ -> },
    viewModel: EventsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var scanToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            if (fx is EventsEffect.ShowError) viewModel.surfaceError(fx.message)
        }
    }

    val launchScanner = rememberQrScanner { result ->
        when (result) {
            is QrScanResult.Success -> {
                val eventId = parseEventCheckInPayload(result.value)
                if (eventId != null) onScannedEvent(eventId)
                else scanToast = "That QR isn't a GRACE event code."
            }
            QrScanResult.Cancelled -> Unit
            is QrScanResult.Error -> scanToast = "⚠ ${result.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "←",
                    color = GraceCream,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
                )
                Column {
                    Text("Events 📅", color = GraceCream, fontSize = 26.sp)
                    Text(
                        "What's happening in our youth community",
                        color = GraceCreamDim,
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "📷 Scan",
                    color = GraceCream, fontSize = 12.sp,
                    modifier = Modifier
                        .background(
                            GraceCardBg,
                            RoundedCornerShape(50)
                        )
                        .clickable { launchScanner() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                if (state.canCreateEvents) {
                    Text(
                        "+ Create",
                        color = GraceDeepBlue, fontSize = 12.sp,
                        modifier = Modifier
                            .background(GraceGold, RoundedCornerShape(50))
                            .clickable { onCreateEvent() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (scanToast != null) {
            Spacer(Modifier.height(10.dp))
            Text(scanToast!!, color = GraceRose, fontSize = 12.sp)
            LaunchedEffect(scanToast) {
                kotlinx.coroutines.delay(2500)
                scanToast = null
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            Text("⚠ ${state.error}", color = GraceRose)
        }

        pendingDeleteId?.let { id ->
            AlertDialog(
                onDismissRequest = { pendingDeleteId = null },
                title = { Text("Delete event?") },
                text = {
                    Text(
                        "This permanently removes the event, RSVPs, and " +
                            "attendance. This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDeleteId = null
                        viewModel.onEvent(EventsEvent.DeleteEvent(id))
                    }) { Text("Delete", color = GraceRose) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        GracePullToRefresh(onRefresh = { viewModel.refresh() }) {
        if (state.events.isEmpty() && !state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📅", fontSize = 44.sp)
                Text(
                    "No upcoming events yet. Check back soon!",
                    color = GraceCreamDim
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        isMyEvent = event.createdBy != null &&
                            event.createdBy == state.currentUserId,
                        canManage = state.canManage(event),
                        isLeader = state.canCreateEvents,
                        onRsvp = { status ->
                            viewModel.onEvent(EventsEvent.Rsvp(event.id, status))
                        },
                        onShowQr = { onShowQr(event.id) },
                        onShowRoster = { onShowRoster(event.id) },
                        onEdit = { onEditEvent(event.id) },
                        onDelete = { pendingDeleteId = event.id },
                        onEmail = {
                            val subject = "📅 ${event.title}"
                            val whenLine = event.eventDate.format(dateFmt)
                            val locLine = if (event.location.isNullOrBlank()) ""
                                else "Location: ${event.location}\n"
                            val desc = event.description?.takeIf { it.isNotBlank() } ?: ""
                            val body = buildString {
                                appendLine("Hi family,")
                                appendLine()
                                appendLine("You're invited to **${event.title}**.")
                                appendLine()
                                appendLine("When: $whenLine")
                                if (locLine.isNotEmpty()) appendLine(locLine.trim())
                                if (desc.isNotEmpty()) {
                                    appendLine()
                                    appendLine(desc)
                                }
                                appendLine()
                                appendLine("Please RSVP in the Royals app. See you there!")
                            }
                            onEmailEvent(subject, body)
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    isMyEvent: Boolean,
    canManage: Boolean,
    isLeader: Boolean,
    onRsvp: (RsvpStatus) -> Unit,
    onShowQr: () -> Unit,
    onShowRoster: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEmail: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    event.title, color = GraceCream, fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                if (event.iHaveAttended) {
                    Text(
                        "✓ Attended",
                        color = GraceGreen, fontSize = 10.sp,
                        modifier = Modifier
                            .background(
                                GraceGreen.copy(alpha = 0.18f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (!event.requiresAttendance) {
                    Text(
                        "📣 Info only",
                        color = GraceCreamDim, fontSize = 10.sp,
                        modifier = Modifier
                            .background(
                                GraceCreamDim.copy(alpha = 0.18f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (canManage) {
                    androidx.compose.foundation.layout.Box {
                        Text(
                            "⋮", color = GraceCreamDim, fontSize = 22.sp,
                            modifier = Modifier
                                .clickable { menuOpen = true }
                                .padding(horizontal = 6.dp)
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { menuOpen = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("📧 Email everyone") },
                                onClick = { menuOpen = false; onEmail() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = GraceRose) },
                                onClick = { menuOpen = false; onDelete() }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "📆 ${event.eventDate.format(dateFmt)}",
                color = GraceGold,
                fontSize = 12.sp
            )
            if (!event.location.isNullOrBlank()) {
                Text("📍 ${event.location}", color = GraceCreamDim, fontSize = 12.sp)
            }
            if (!event.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    event.description,
                    color = GraceCreamDim,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "${event.goingCount} going",
                color = GraceGreen,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RsvpPill("Going", GraceGreen, event.myRsvp == RsvpStatus.GOING) {
                    onRsvp(RsvpStatus.GOING)
                }
                RsvpPill("Maybe", GraceGold, event.myRsvp == RsvpStatus.MAYBE) {
                    onRsvp(RsvpStatus.MAYBE)
                }
                RsvpPill("Can't go", GraceRose, event.myRsvp == RsvpStatus.NOT_GOING) {
                    onRsvp(RsvpStatus.NOT_GOING)
                }
            }

            if (isMyEvent && event.requiresAttendance) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "🎫 Show attendance QR",
                    color = GraceDeepBlue,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(GraceGold, RoundedCornerShape(50))
                        .clickable { onShowQr() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            if (isLeader && event.requiresAttendance) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "👥 Mark my cell's attendance",
                    color = GracePurple,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(
                            GracePurple.copy(alpha = 0.18f),
                            RoundedCornerShape(50)
                        )
                        .clickable { onShowRoster() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RsvpPill(
    label: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) accent else accent.copy(alpha = 0.12f),
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) GraceDeepBlue else accent,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun parseEventCheckInPayload(raw: String): String? {
    val uri = runCatching { android.net.Uri.parse(raw.trim()) }.getOrNull()
        ?: return null
    if (!uri.scheme.equals("grace", ignoreCase = true)) return null
    if (!uri.host.equals("event-checkin", ignoreCase = true)) return null
    return uri.lastPathSegment?.takeIf { it.isNotBlank() }
}
