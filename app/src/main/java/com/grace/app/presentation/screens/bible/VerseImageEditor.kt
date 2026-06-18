package com.grace.app.presentation.screens.bible

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.grace.app.data.util.VerseImageRenderer
import com.grace.app.presentation.components.GraceButton
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VerseImageEditor(
    verseText: String,
    reference: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var font by remember { mutableStateOf(VerseImageRenderer.CardFont.SERIF) }
    var bgIndex by remember { mutableStateOf(0) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) imageUri = uri }

    LaunchedEffect(font, bgIndex, imageUri) {
        bitmap = withContext(Dispatchers.Default) {
            VerseImageRenderer.render(
                context = context,
                verseText = verseText,
                reference = reference,
                background = if (imageUri == null) VerseImageRenderer.backgrounds[bgIndex] else null,
                font = font,
                imageUri = imageUri
            )
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
    Box(modifier = Modifier.fillMaxSize().background(GraceDeepBlue)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "←", color = GraceCream, fontSize = 22.sp,
                    modifier = Modifier.clickable { onClose() }.padding(end = 12.dp)
                )
                Text("Verse image", color = GraceCream, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GraceCardBg),
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Verse image preview",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(color = GraceGold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Label("BACKGROUND")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerseImageRenderer.backgrounds.forEachIndexed { i, bg ->
                    StyleChip(
                        label = bg.name,
                        selected = imageUri == null && bgIndex == i,
                        onClick = { imageUri = null; bgIndex = i }
                    )
                }
                StyleChip(
                    label = "🖼 Gallery",
                    selected = imageUri != null,
                    onClick = {
                        gallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            Label("FONT")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerseImageRenderer.CardFont.entries.forEach { f ->
                    StyleChip(label = f.label, selected = font == f, onClick = { font = f })
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GraceButton(
                    text = "Share",
                    onClick = {
                        bitmap?.let { bmp ->
                            VerseImageRenderer.shareIntent(context, bmp)
                                ?.let { context.startActivity(it) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = GraceGold,
                    contentColor = GraceDeepBlue
                )
                GraceButton(
                    text = "Save",
                    onClick = {
                        bitmap?.let { bmp ->
                            toast = if (VerseImageRenderer.saveToGallery(context, bmp) != null)
                                "Saved to your gallery ✓" else "Couldn't save."
                        }
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = GraceCardBg,
                    contentColor = GraceCream
                )
            }
            if (toast != null) {
                Spacer(Modifier.height(10.dp))
                Text(toast!!, color = GraceGreen, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text, color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) GraceDeepBlue else GraceCream,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(if (selected) GraceGold else GraceCardBg, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
