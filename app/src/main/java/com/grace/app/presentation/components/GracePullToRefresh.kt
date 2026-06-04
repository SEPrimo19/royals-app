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

/**
 * Pull-to-refresh wrapper for any scrollable content.
 *
 * Wraps the [content] in a nested-scroll Box so a downward pull triggers
 * [onRefresh]. After the refresh callback returns, the spinner is held for
 * at least [MIN_SPINNER_MS] so the user gets visible feedback even on
 * instant cache-only refreshes.
 *
 * Wrap only the scrollable region (not non-scrolling page headers), so the
 * spinner overlay doesn't cover header text.
 *
 * Material3 1.2.x's [PullToRefreshContainer] is experimental but stable
 * enough for production. Migrate to 1.3+'s `PullToRefreshBox` when we bump
 * the Compose BOM.
 */
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
            // ViewModels' refresh() is fire-and-forget — it starts a new
            // collection but returns instantly. So we time the spinner to
            // guarantee visible feedback rather than racing the network.
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
        // Material3 1.2.x's PullToRefreshContainer renders its idle track
        // even when not pulling — on Light theme that's a visible white
        // circle hovering near the top. Only render the container when
        // the user is actively dragging OR a refresh is in flight, so the
        // indicator appears as soon as they start pulling and disappears
        // again when idle.
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
