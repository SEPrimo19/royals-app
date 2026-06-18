package com.grace.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceGold

@Composable
fun PlaceholderScreen(title: String, builtIn: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✦ $title",
            style = MaterialTheme.typography.displayMedium,
            color = GraceGold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Built in $builtIn",
            style = MaterialTheme.typography.bodyLarge,
            color = GraceCreamDim,
            textAlign = TextAlign.Center
        )
    }
}
