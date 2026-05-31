package com.streamvault.app.ui.screens.f1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.streamvault.app.R
import com.streamvault.app.ui.components.ChannelLogoBadge
import com.streamvault.app.ui.components.FocusedMarqueeText
import com.streamvault.app.ui.components.MovieCard
import com.streamvault.app.ui.components.SelectionChip
import com.streamvault.app.ui.components.SelectionChipRow
import com.streamvault.app.ui.components.SeriesCard
import com.streamvault.app.ui.components.TvEmptyState
import com.streamvault.app.ui.components.shell.AppNavigationChrome
import com.streamvault.app.ui.components.shell.AppScreenScaffold
import com.streamvault.app.ui.components.shell.AppSectionHeader
import com.streamvault.app.ui.components.shell.StatusPill
import com.streamvault.app.ui.design.AppColors
import com.streamvault.app.ui.interaction.rememberTvInteractionSounds
import com.streamvault.app.ui.theme.FocusBorder
import com.streamvault.app.ui.theme.OnSurface
import com.streamvault.app.ui.theme.OnSurfaceDim
import com.streamvault.app.ui.theme.SurfaceElevated
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.F1DetectedChannel
import com.streamvault.domain.model.F1FeedType
import com.streamvault.domain.model.F1RaceWeekend
import com.streamvault.domain.model.F1Session
import com.streamvault.domain.model.F1SessionStatus
import com.streamvault.domain.model.F1SessionType
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Series
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val TAB_MAIN = "main"
private const val TAB_ONBOARD = "onboard"
private const val TAB_PIT_LANE = "pit_lane"

@Composable
fun F1Screen(
    onNavigate: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit,
    currentRoute: String,
    viewModel: F1ViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(TAB_MAIN) }

    AppScreenScaffold(
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = stringResource(R.string.f1_screen_title),
        navigationChrome = AppNavigationChrome.TopBar,
        showScreenHeader = false
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Race weekend header
            state.currentOrNextWeekend?.let { weekend ->
                item(key = "race_header") {
                    RaceWeekendHeader(weekend = weekend, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
                }
                item(key = "session_row") {
                    SessionChipRow(sessions = weekend.sessions, modifier = Modifier.padding(bottom = 8.dp))
                }
            }

            if (state.calendarError != null && state.currentOrNextWeekend == null) {
                item(key = "calendar_error") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.f1_no_calendar),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceDim
                        )
                    }
                }
            }

            // ── Live channels ──────────────────────────────────────────────
            item(key = "channels_header") {
                AppSectionHeader(
                    title = stringResource(R.string.f1_live_section),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!state.isChannelsLoading && !state.channels.isEmpty) {
                item(key = "channel_tabs") {
                    val chips = buildList {
                        if (state.channels.main.isNotEmpty()) add(SelectionChip(TAB_MAIN, stringResource(R.string.f1_tab_main)))
                        if (state.channels.onboard.isNotEmpty()) add(SelectionChip(TAB_ONBOARD, stringResource(R.string.f1_tab_onboard)))
                        if (state.channels.pitLane.isNotEmpty()) add(SelectionChip(TAB_PIT_LANE, stringResource(R.string.f1_tab_pit_lane)))
                    }
                    val effectiveTab = if (chips.any { it.key == selectedTab }) selectedTab else chips.firstOrNull()?.key ?: TAB_MAIN
                    if (effectiveTab != selectedTab) selectedTab = effectiveTab
                    SelectionChipRow(
                        title = "",
                        chips = chips,
                        selectedKey = selectedTab,
                        onChipSelected = { selectedTab = it }
                    )
                }

                val visibleChannels = when (selectedTab) {
                    TAB_ONBOARD -> state.channels.onboard
                    TAB_PIT_LANE -> state.channels.pitLane
                    else -> state.channels.main
                }

                items(visibleChannels, key = { it.channel.id }) { detected ->
                    F1ChannelCard(
                        detected = detected,
                        onClick = { onPlayChannel(detected.channel) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            } else if (!state.isChannelsLoading && state.channels.isEmpty) {
                item(key = "no_channels") {
                    Box(modifier = Modifier.height(80.dp)) {
                        TvEmptyState(
                            title = stringResource(R.string.f1_no_channels),
                            subtitle = stringResource(R.string.f1_no_channels_subtitle)
                        )
                    }
                }
            }

            // ── Movies ─────────────────────────────────────────────────────
            item(key = "movies_header") {
                AppSectionHeader(
                    title = stringResource(R.string.f1_movies_section),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (!state.isMoviesLoading && state.movies.isNotEmpty()) {
                item(key = "movies_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.movies, key = { "movie_${it.id}" }) { movie ->
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie) }
                            )
                        }
                    }
                }
            } else if (!state.isMoviesLoading) {
                item(key = "no_movies") {
                    Box(modifier = Modifier.height(80.dp)) {
                        TvEmptyState(
                            title = stringResource(R.string.f1_no_movies),
                            subtitle = ""
                        )
                    }
                }
            }

            // ── Series ─────────────────────────────────────────────────────
            item(key = "series_header") {
                AppSectionHeader(
                    title = stringResource(R.string.f1_series_section),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (!state.isSeriesLoading && state.series.isNotEmpty()) {
                item(key = "series_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.series, key = { "series_${it.id}" }) { series ->
                            SeriesCard(
                                series = series,
                                onClick = { onSeriesClick(series) }
                            )
                        }
                    }
                }
            } else if (!state.isSeriesLoading) {
                item(key = "no_series") {
                    Box(modifier = Modifier.height(80.dp)) {
                        TvEmptyState(
                            title = stringResource(R.string.f1_no_series),
                            subtitle = ""
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RaceWeekendHeader(weekend: F1RaceWeekend, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = weekend.raceName,
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${weekend.circuitName} · ${weekend.country}",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SessionChipRow(sessions: List<F1Session>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions, key = { it.type.name }) { session ->
            SessionStatusChip(session = session)
        }
    }
}

@Composable
private fun SessionStatusChip(session: F1Session) {
    val containerColor = when (session.status) {
        F1SessionStatus.LIVE -> AppColors.Live
        F1SessionStatus.SCHEDULED -> AppColors.Brand.copy(alpha = 0.18f)
        F1SessionStatus.COMPLETED -> AppColors.SurfaceElevated
    }
    val contentColor = when (session.status) {
        F1SessionStatus.LIVE -> AppColors.TextPrimary
        F1SessionStatus.SCHEDULED -> AppColors.Brand
        F1SessionStatus.COMPLETED -> AppColors.TextTertiary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (session.status == F1SessionStatus.LIVE) {
                StatusPill(
                    label = "LIVE",
                    containerColor = AppColors.Live,
                    contentColor = AppColors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = sessionLabel(session.type),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
            if (session.status != F1SessionStatus.LIVE) {
                Text(
                    text = formatSessionTime(session.startTimeUtcMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun F1ChannelCard(
    detected: F1DetectedChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sounds = rememberTvInteractionSounds()
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Surface(
        onClick = {
            sounds.playSelect()
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = AppColors.SurfaceEmphasis
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChannelLogoBadge(
                logoUrl = detected.channel.logoUrl,
                channelName = detected.channel.name,
                modifier = Modifier
                    .height(40.dp)
                    .width(64.dp)
            )
            FocusedMarqueeText(
                text = detected.channel.name,
                isFocused = isFocused,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface,
                modifier = Modifier.weight(1f)
            )
            if (detected.feedType != F1FeedType.MAIN) {
                StatusPill(
                    label = feedTypeLabel(detected.feedType),
                    containerColor = AppColors.SurfaceAccent,
                    contentColor = AppColors.TextSecondary
                )
            }
        }
    }
}

private fun sessionLabel(type: F1SessionType): String = when (type) {
    F1SessionType.FP1 -> "FP1"
    F1SessionType.FP2 -> "FP2"
    F1SessionType.FP3 -> "FP3"
    F1SessionType.SPRINT_QUALIFYING -> "Sprint Q"
    F1SessionType.SPRINT -> "Sprint"
    F1SessionType.QUALIFYING -> "Qualifying"
    F1SessionType.RACE -> "Race"
}

private fun feedTypeLabel(type: F1FeedType): String = when (type) {
    F1FeedType.MAIN -> "Main"
    F1FeedType.ONBOARD -> "Onboard"
    F1FeedType.PIT_LANE -> "Pit Lane"
}

private val timeFormatter = SimpleDateFormat("EEE HH:mm", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
}

private fun formatSessionTime(utcMs: Long): String =
    timeFormatter.format(Date(utcMs))
