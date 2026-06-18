package com.grace.app.presentation.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.grace.app.domain.model.AppVersion
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose

@Composable
fun UpdateBanner(viewModel: UpdateBannerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWhatsNew by remember { mutableStateOf(false) }

    fun openDownload(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    if (state.showBlocker) {
        val update = state.update ?: return
        MandatoryUpdateBlocker(
            update = update,
            onDownload = { openDownload(update.downloadUrl) }
        )
        return
    }

    if (state.showBanner) {
        val update = state.update ?: return
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = GraceCardAlt),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 18.sp)
                    Spacer(Modifier.padding(start = 8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Royals ${update.versionName} is available",
                            color = GraceCream, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!update.releaseNotes.isNullOrBlank()) {
                            Spacer(Modifier.padding(top = 2.dp))
                            Text(
                                update.releaseNotes,
                                color = GraceCreamDim, fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
                Spacer(Modifier.padding(top = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.dismiss() }) {
                        Text("Later", color = GraceCreamDim, fontSize = 13.sp)
                    }
                    Spacer(Modifier.padding(start = 4.dp))
                    Text(
                        "Download",
                        color = GraceDeepBlue, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(GraceGold, RoundedCornerShape(50))
                            .clickable { showWhatsNew = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (showWhatsNew) {
            WhatsNewDialog(
                update = update,
                onDownload = {
                    openDownload(update.downloadUrl)
                    showWhatsNew = false
                },
                onDismiss = { showWhatsNew = false }
            )
        }
    }
}

@Composable
private fun WhatsNewDialog(
    update: AppVersion,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "✨ Royals ${update.versionName}",
                color = GraceCream, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "WHAT'S NEW",
                    color = GraceGold, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.padding(top = 6.dp))
                Text(
                    update.releaseNotes?.takeIf { it.isNotBlank() }
                        ?: "Improvements and fixes.",
                    color = GraceCream, fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Text(
                "Download",
                color = GraceDeepBlue, fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(GraceGold, RoundedCornerShape(50))
                    .clickable { onDownload() }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = GraceCreamDim, fontSize = 13.sp)
            }
        }
    )
}

@Composable
private fun MandatoryUpdateBlocker(
    update: AppVersion,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {   },
        title = {
            Text(
                "Update Required",
                color = GraceRose, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Royals ${update.versionName} is a required update. " +
                        "Please download it before continuing.",
                    color = GraceCream, fontSize = 14.sp
                )
                if (!update.releaseNotes.isNullOrBlank()) {
                    Spacer(Modifier.padding(top = 10.dp))
                    Text(
                        "What's in this update:",
                        color = GraceCreamDim, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.padding(top = 4.dp))
                    Text(
                        update.releaseNotes,
                        color = GraceCreamDim, fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text(
                    "Download",
                    color = GraceGold, fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
