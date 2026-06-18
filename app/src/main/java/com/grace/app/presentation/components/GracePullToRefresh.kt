package com.grace.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceGold
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GracePullToRefresh(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(Unit) {
            val started = System.currentTimeMillis()
            onRefresh()
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < MIN_SPINNER_MS) {
                delay(MIN_SPINNER_MS - elapsed)
            }
            state.endRefresh()
        }
    }
    Box(modifier = modifier.nestedScroll(state.nestedScrollConnection)) {
        content()
        if (state.verticalOffset > 0.5f || state.isRefreshing) {
            PullToRefreshContainer(
                state = state,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = GraceCardBg,
                contentColor = GraceGold
            )
        }
    }
}

private const val MIN_SPINNER_MS = 500L
