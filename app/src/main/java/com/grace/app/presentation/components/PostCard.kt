package com.grace.app.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grace.app.domain.model.Post
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import java.time.Duration
import java.time.LocalDateTime

@Composable
private fun ExpandableText(content: String) {
    var expanded by remember(content) { mutableStateOf(false) }
    var overflowing by remember(content) { mutableStateOf(false) }

    Column(modifier = Modifier.animateContentSize()) {
        Text(
            content,
            color = GraceCreamDim,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 6,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) overflowing = result.hasVisualOverflow
            }
        )
        if (overflowing || expanded) {
            Text(
                if (expanded) "Read less" else "Read more",
                color = GraceGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { expanded = !expanded }
            )
        }
    }
}

private fun ago(t: LocalDateTime): String {
    val d = Duration.between(t, LocalDateTime.now())
    return when {
        d.toMinutes() < 1 -> "just now"
        d.toMinutes() < 60 -> "${d.toMinutes()}m ago"
        d.toHours() < 24 -> "${d.toHours()}h ago"
        else -> "${d.toDays()}d ago"
    }
}

private data class ReactionDef(val type: String, val emoji: String, val label: String)

private val reactionDefs = listOf(
    ReactionDef("pray", "🙏", "Praying"),
    ReactionDef("fire", "🔥", "This hit"),
    ReactionDef("amen", "✝️", "Amen")
)

@Composable
fun PostCard(
    post: Post,
    myReaction: String?,
    onReact: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (post.isHighlighted) {
                Text("✦ Spotlighted by Leader", color = GraceGold, fontSize = 10.sp)
                Spacer(Modifier.size(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(GraceGreen.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        post.userName.firstOrNull()?.uppercase() ?: "?",
                        color = GraceCream
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        post.userName.ifBlank { "A Youth" },
                        color = GraceCream,
                        fontSize = 14.sp
                    )
                    Text(ago(post.createdAt), color = GraceCreamDim, fontSize = 11.sp)
                }
            }

            if (post.verseRef != null) {
                Spacer(Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .background(GraceGold.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "📖 ${post.verseRef}",
                        color = GraceGold,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            if (post.content.isNotBlank()) {
                Spacer(Modifier.size(10.dp))
                ExpandableText(post.content)
            }

            if (post.imageUrl != null) {
                Spacer(Modifier.size(10.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(Modifier.size(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reactionDefs.forEach { r ->
                    val active = myReaction == r.type
                    Box(
                        modifier = Modifier
                            .background(
                                if (active) GraceGreen.copy(alpha = 0.2f) else GraceCardBg,
                                RoundedCornerShape(50)
                            )
                            .clickable { onReact(r.type) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${r.emoji} ${post.reactions[r.type] ?: 0}",
                            color = if (active) GraceGreen else GraceCreamDim,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
