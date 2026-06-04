package com.grace.app.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grace.app.presentation.theme.GraceCreamDim

/**
 * Burger-icon row planted at the top of every bottom-nav tab screen so users
 * can open the app-level drawer from anywhere.
 *
 * Position contract: the icon glyph sits at exactly 16dp from the screen's
 * left edge on every tab. IconButton has 12dp of internal padding, so a 4dp
 * start padding on the Row produces the target 16dp gap. Caller MUST place
 * MenuButtonRow at the screen edge (NOT inside another horizontally padded
 * container) — otherwise the burger's offset stacks with the parent's
 * padding and drifts inward.
 */
@Composable
fun MenuButtonRow(onOpenMenu: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(Icons.Filled.Menu, "Menu", tint = GraceCreamDim)
        }
    }
}
