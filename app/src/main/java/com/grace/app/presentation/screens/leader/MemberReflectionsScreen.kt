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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.MeditationTheme
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
import java.time.format.DateTimeFormatter

private val reflectionDateFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun MemberReflectionsScreen(
    onBack: () -> Unit,
    viewModel: MemberReflectionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val memberName = state.mentee?.user?.name?.ifBlank { null }
        ?: state.mentee?.user?.email
        ?: "Member"

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
                Text("Reflections", color = GraceCream, fontSize = 22.sp)
                Text(
                    "from $memberName",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize(), Alignment.Center
            ) { CircularProgressIndicator(color = GraceGold) }

            state.items.isEmpty() -> Box(
                Modifier.fillMaxSize(), Alignment.Center
            ) {
                Text(
                    "This member hasn't shared a meditation reflection yet.",
                    color = GraceCreamDim, fontSize = 13.sp
                )
            }

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.submission.id }) { item ->
                    ReflectionCard(item)
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ReflectionCard(item: MemberReflectionItem) {
    val meditation = item.meditation
    val themeColor = meditation?.theme?.let { themeColor(it) } ?: GraceCreamDim
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (meditation != null) {
                    Text(
                        meditation.theme.label,
                        color = themeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                themeColor.copy(alpha = 0.18f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "Week ${meditation.weekNumber}",
                        color = GraceCreamDim, fontSize = 10.sp
                    )
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        item.submission.submittedAt.format(reflectionDateFmt),
                        color = GraceCreamDim, fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
            if (meditation != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    meditation.title,
                    color = GraceCream, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    meditation.scriptureRef,
                    color = GraceCreamDim, fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.submission.reflectionText,
                color = GraceCream, fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun themeColor(theme: MeditationTheme) = when (theme) {
    MeditationTheme.JESUS -> GraceGold
    MeditationTheme.EDUCATION -> GraceBlue
    MeditationTheme.FAMILY -> GraceGreen
    MeditationTheme.FRIENDS -> GracePurple
    MeditationTheme.CHURCH -> GraceRose
    MeditationTheme.RELATIONSHIPS -> GraceOrange
}
