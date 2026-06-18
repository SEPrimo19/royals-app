package com.grace.app.presentation.screens.lifegroup

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.grace.app.domain.model.LifeGroupDetail
import com.grace.app.domain.model.User
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

@Composable
fun LifeGroupScreen(
    onBack: () -> Unit,
    viewModel: LifeGroupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingRemoveUser by remember { mutableStateOf<User?>(null) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            toast = when (fx) {
                is LifeGroupEffect.ShowError -> "⚠ ${fx.message}"
                is LifeGroupEffect.ShowSuccess -> "✓ ${fx.message}"
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
            Text("My Life Group 🤝", color = GraceCream, fontSize = 24.sp)
        }

        if (toast != null) {
            Spacer(Modifier.height(10.dp))
            Text(toast!!, color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose)
        }

        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading && state.detail == null ->
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }

            state.error != null && state.detail == null ->
                LoadFailedView(
                    message = state.error!!,
                    onRetry = { viewModel.refresh() }
                )

            state.detail == null && state.canCreate ->
                CreateGroupForm(
                    isWorking = state.isWorking,
                    onSubmit = { name, desc ->
                        viewModel.onEvent(LifeGroupEvent.CreateGroup(name, desc))
                    }
                )

            state.detail == null ->
                EmptyMemberState()

            else -> {
                if (state.error != null) {
                    Text("⚠ ${state.error}", color = GraceRose)
                    Spacer(Modifier.height(10.dp))
                }
                GroupView(
                    detail = state.detail!!,
                    myUserId = state.myUserId,
                    canManage = state.canManage,
                    onAdd = { showAddDialog = true },
                    onRemove = { pendingRemoveUser = it },
                    onLeave = { showLeaveDialog = true },
                    onDelete = { showDeleteDialog = true }
                )

                if (state.canManage) {
                    Spacer(Modifier.height(20.dp))
                    JoinRequestsInbox(groupId = state.detail!!.group.id)
                }

                if (state.canCreateSecondCell) {
                    Spacer(Modifier.height(24.dp))
                    if (state.showCreateForm) {
                        Text(
                            "CREATE YOUR OWN CELL",
                            color = GraceGold, fontSize = 11.sp,
                            letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        CreateGroupForm(
                            isWorking = state.isWorking,
                            onSubmit = { name, desc ->
                                viewModel.onEvent(LifeGroupEvent.CreateGroup(name, desc))
                            }
                        )
                    } else {
                        Text(
                            "＋ Create your own cell",
                            color = GraceGold, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    viewModel.onEvent(LifeGroupEvent.ToggleCreateForm)
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showAddDialog) {
        SearchMemberDialog(
            query = state.searchQuery,
            results = state.searchResults,
            isSearching = state.isSearching,
            onQueryChange = { viewModel.onEvent(LifeGroupEvent.SearchQueryChanged(it)) },
            onPick = { user ->
                showAddDialog = false
                viewModel.onEvent(LifeGroupEvent.AddMember(user.id))
            },
            onBrowseAll = {
                showAddDialog = false
                viewModel.onEvent(LifeGroupEvent.ClearSearch)
                viewModel.onEvent(LifeGroupEvent.OpenBrowse)
            },
            onDismiss = {
                showAddDialog = false
                viewModel.onEvent(LifeGroupEvent.ClearSearch)
            }
        )
    }

    if (state.browseOpen) {
        BrowseMembersDialog(
            filter = state.browseFilter,
            users = state.browseFiltered,
            totalCount = state.browseAll.size,
            isLoading = state.isBrowseLoading,
            onFilterChange = {
                viewModel.onEvent(LifeGroupEvent.BrowseFilterChanged(it))
            },
            onPick = { user ->
                viewModel.onEvent(LifeGroupEvent.AddMember(user.id))
            },
            onDismiss = { viewModel.onEvent(LifeGroupEvent.CloseBrowse) }
        )
    }

    pendingRemoveUser?.let { u ->
        AlertDialog(
            onDismissRequest = { pendingRemoveUser = null },
            title = { Text("Remove ${u.name}?") },
            text = { Text("They'll be removed from this Life Group.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRemoveUser = null
                    viewModel.onEvent(LifeGroupEvent.RemoveMember(u.id))
                }) { Text("Remove", color = GraceRose) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveUser = null }) { Text("Cancel") }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave this Life Group?") },
            text = { Text("You can be added back later by your leader.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    viewModel.onEvent(LifeGroupEvent.LeaveGroup)
                }) { Text("Leave", color = GraceRose) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this cell group?") },
            text = {
                Text(
                    "This permanently removes the cell group. All current " +
                        "members will become unassigned and can request to " +
                        "join another cell. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.onEvent(LifeGroupEvent.DeleteGroup)
                }) { Text("Delete", color = GraceRose) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CreateGroupForm(isWorking: Boolean, onSubmit: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Create your Life Group", color = GraceCream, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Name your group and start adding members.",
                color = GraceCreamDim, fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. Friday Night Warriors", color = GraceCreamDim) }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Short description (optional)", color = GraceCreamDim)
                }
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSubmit(name, desc) },
                enabled = !isWorking && name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isWorking) CircularProgressIndicator(
                    color = GraceDeepBlue, modifier = Modifier.size(18.dp)
                ) else Text("Create Life Group", color = GraceDeepBlue)
            }
        }
    }
}

@Composable
private fun LoadFailedView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚠", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(message, color = GraceRose, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = GraceGold)
        ) { Text("Try again", color = GraceDeepBlue) }
    }
}

@Composable
private fun EmptyMemberState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌱", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "You're not in a Life Group yet.",
            color = GraceCream, fontSize = 16.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Ask your cell leader to add you, " +
                "or talk to a pastor about joining one.",
            color = GraceCreamDim, fontSize = 12.sp
        )
    }
}

@Composable
private fun GroupView(
    detail: LifeGroupDetail,
    myUserId: String?,
    canManage: Boolean,
    onAdd: () -> Unit,
    onRemove: (User) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(detail.group.name, color = GraceCream, fontSize = 20.sp,
                fontWeight = FontWeight.Bold)
            if (!detail.group.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(detail.group.description!!, color = GraceCreamDim, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${detail.memberCount} member${if (detail.memberCount == 1) "" else "s"}",
                color = GraceGold, fontSize = 11.sp, letterSpacing = 2.sp
            )
        }
    }

    if (detail.leader != null) {
        Spacer(Modifier.height(14.dp))
        SectionHeader("LEADER")
        Spacer(Modifier.height(8.dp))
        MemberRow(
            user = detail.leader,
            isLeader = true,
            canRemove = false,
            onRemove = {}
        )
    }

    Spacer(Modifier.height(14.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader("MEMBERS")
        if (canManage) {
            Text(
                "+ Add",
                color = GraceGold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(GraceGold.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .clickable { onAdd() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    val nonLeader = detail.members.filter { it.id != detail.group.leaderId }
    if (nonLeader.isEmpty()) {
        Text(
            "No other members yet. Tap + Add to invite one.",
            color = GraceCreamDim, fontSize = 12.sp
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height((nonLeader.size * 64).dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(nonLeader, key = { it.id }) { member ->
                MemberRow(
                    user = member,
                    isLeader = false,
                    canRemove = canManage,
                    onRemove = { onRemove(member) }
                )
            }
        }
    }

    val isMember = myUserId != null && detail.members.any { it.id == myUserId }
    val isLeader = myUserId != null && detail.group.leaderId == myUserId
    if (isMember && !isLeader) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Leave this Life Group",
            color = GraceRose,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLeave() }
                .padding(vertical = 10.dp)
        )
    }
    if (canManage) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Delete this cell group",
            color = GraceRose,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDelete() }
                .padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun MemberRow(
    user: User,
    isLeader: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(
                        if (isLeader) GraceGold.copy(alpha = 0.35f)
                        else GraceMuted.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.name.firstOrNull()?.uppercase() ?: "?",
                    color = GraceCream
                )
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name.ifBlank { "—" }, color = GraceCream, fontSize = 14.sp)
                Text(user.email, color = GraceCreamDim, fontSize = 11.sp)
            }

            val msg = user.messengerUrl
            if (user.messengerPublic && !msg.isNullOrBlank()) {
                Text(
                    "💬",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, msg.toUri())
                                )
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (canRemove) {
                Text(
                    "Remove",
                    color = GraceRose, fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { onRemove() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchMemberDialog(
    query: String,
    results: List<User>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onPick: (User) -> Unit,
    onBrowseAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a member") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Search by name or email. Only members who aren't " +
                        "already in a Life Group will appear.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Type at least 2 characters…", color = GraceCreamDim) }
                )
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onBrowseAll,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Or browse all members without a Life Group →",
                        color = GraceGold, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                when {
                    query.trim().length < 2 -> {
                    }
                    isSearching -> Box(
                        Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = GraceGold) }
                    results.isEmpty() -> Text(
                        "No matches. They might not be signed up yet — or " +
                            "they're already in another Life Group.",
                        color = GraceCreamDim, fontSize = 12.sp
                    )
                    else -> Column(modifier = Modifier.fillMaxWidth()) {
                        results.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(user) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            GraceMuted.copy(alpha = 0.4f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        user.name.firstOrNull()?.uppercase() ?: "?",
                                        color = GraceCream
                                    )
                                }
                                Spacer(Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, color = GraceCream, fontSize = 14.sp)
                                    Text(
                                        "Member",
                                        color = GraceCreamDim, fontSize = 11.sp
                                    )
                                }
                                Text("+ Add", color = GraceGold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun BrowseMembersDialog(
    filter: String,
    users: List<User>,
    totalCount: Int,
    isLoading: Boolean,
    onFilterChange: (String) -> Unit,
    onPick: (User) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Browse members") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Everyone here is a member without a Life Group. Tap " +
                        "+ Add to bring them into yours.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = filter,
                    onValueChange = onFilterChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text("Filter by name or email…", color = GraceCreamDim)
                    }
                )
                Spacer(Modifier.height(10.dp))
                when {
                    isLoading -> Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = GraceGold) }
                    totalCount == 0 -> Text(
                        "No groupless members right now. New sign-ups " +
                            "will appear here automatically.",
                        color = GraceCreamDim, fontSize = 12.sp
                    )
                    users.isEmpty() -> Text(
                        "No matches for \"$filter\".",
                        color = GraceCreamDim, fontSize = 12.sp
                    )
                    else -> {
                        Text(
                            "${users.size} of $totalCount",
                            color = GraceCreamDim, fontSize = 11.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(360.dp)
                        ) {
                            items(users, key = { it.id }) { user ->
                                BrowseMemberRow(user = user, onAdd = { onPick(user) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun BrowseMemberRow(user: User, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(GraceMuted.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                user.name.firstOrNull()?.uppercase() ?: "?",
                color = GraceCream
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, color = GraceCream, fontSize = 14.sp)
            Text(user.email, color = GraceCreamDim, fontSize = 11.sp,
                maxLines = 1)
        }
        Text("+ Add", color = GraceGold, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label, color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 3.sp, fontWeight = FontWeight.Bold
    )
}
