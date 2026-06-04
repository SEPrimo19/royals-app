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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.presentation.components.categoryColor
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PostPrayerOnBehalfScreen(
    onBack: () -> Unit,
    onPosted: () -> Unit,
    viewModel: PostPrayerOnBehalfViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { fx ->
            if (fx is PostPrayerOnBehalfEffect.Posted) onPosted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Post Prayer", color = GraceCream, fontSize = 22.sp)
                Text(
                    "On behalf of this member",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        // Honesty notice — the leader needs to know community will see
        // the "via {leader}" tag, so they don't post things the member
        // wanted kept private.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = GraceGold.copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "This prayer will appear on the Prayer Wall tagged \"via you\" " +
                    "so the community knows it came through a leader. " +
                    "Anonymous posting is not available for proxy prayers.",
                color = GraceCream, fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Prayer request",
            color = GraceCreamDim, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = state.content,
            onValueChange = {
                viewModel.onEvent(PostPrayerOnBehalfEvent.ContentChanged(it))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            placeholder = {
                Text(
                    "What would the community pray for on this member's behalf?",
                    color = GraceCreamDim
                )
            }
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Category",
            color = GraceCreamDim, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PrayerCategory.entries.toList()) { cat ->
                val selected = cat == state.category
                val color = categoryColor(cat)
                Text(
                    cat.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = if (selected) GraceDeepBlue else color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            if (selected) color else color.copy(alpha = 0.18f),
                            RoundedCornerShape(50)
                        )
                        .clickable {
                            viewModel.onEvent(PostPrayerOnBehalfEvent.CategoryChanged(cat))
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text("⚠ ${state.error}", color = GraceRose, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.onEvent(PostPrayerOnBehalfEvent.Submit) },
            enabled = state.canSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (state.isSubmitting) "Posting…" else "Post to Prayer Wall",
                color = GraceDeepBlue, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
