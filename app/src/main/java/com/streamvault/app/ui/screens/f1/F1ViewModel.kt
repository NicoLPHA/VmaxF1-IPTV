package com.streamvault.app.ui.screens.f1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.f1.F1ChannelMatcher
import com.streamvault.data.remote.f1.computeSessionStatus
import com.streamvault.domain.model.F1RaceWeekend
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.F1CalendarRepository
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val f1IncludePatterns = listOf(
    "formula 1", "formula1", "formule 1", "formule1",
    "f1 tv", "f1tv", "f1 tv pro", "f1tvpro",
    "sky f1", "sky sports f1", "grand prix",
    "viaplay f1", "viaplay formule 1", "viaplay formula 1"
)
private val f1ExcludePatterns = listOf("ryan hamilton", "nicky verstappen", "nick verstappen")
private val f1StandaloneRegex = Regex("""(?<![a-z0-9])f1(?![a-z0-9])""")

private fun isF1Content(name: String): Boolean {
    val lower = name.lowercase()
    if (f1ExcludePatterns.any { lower.contains(it) }) return false
    if (f1IncludePatterns.any { lower.contains(it) }) return true
    return f1StandaloneRegex.containsMatchIn(lower)
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class F1ViewModel @Inject constructor(
    private val f1CalendarRepository: F1CalendarRepository,
    private val channelRepository: ChannelRepository,
    private val providerRepository: ProviderRepository,
    private val f1ChannelMatcher: F1ChannelMatcher,
    private val movieRepository: MovieRepository,
    private val seriesRepository: SeriesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(F1ScreenState())
    val state: StateFlow<F1ScreenState> = _state.asStateFlow()

    init {
        loadCalendar(forceRefresh = false)
        loadChannels()
        loadMovies()
        loadSeries()
        startClockTick()
    }

    fun refresh() {
        loadCalendar(forceRefresh = true)
    }

    private fun loadCalendar(forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isCalendarLoading = true, calendarError = null) }
            val nowMs = System.currentTimeMillis()
            when (val result = f1CalendarRepository.getCalendar(forceRefresh)) {
                is Result.Success -> {
                    val calendar = result.data
                    val races = recomputeStatuses(calendar.races, nowMs)
                    val currentOrNext = resolveCurrentOrNext(races, nowMs)
                    _state.update {
                        it.copy(
                            isCalendarLoading = false,
                            allRaces = races,
                            currentOrNextWeekend = currentOrNext,
                            calendarError = null
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(isCalendarLoading = false, calendarError = result.message)
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun loadChannels() {
        viewModelScope.launch {
            providerRepository.getProviders()
                .flatMapLatest { providers ->
                    if (providers.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val flows = providers.map { channelRepository.getChannels(it.id) }
                        combine(flows) { arrays -> arrays.flatMap { it } }
                    }
                }
                .collect { allChannels ->
                    val group = f1ChannelMatcher.match(allChannels)
                    _state.update { it.copy(channels = group, isChannelsLoading = false) }
                }
        }
    }

    private fun loadMovies() {
        viewModelScope.launch {
            providerRepository.getProviders()
                .flatMapLatest { providers ->
                    if (providers.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val flows = providers.map { movieRepository.getMovies(it.id) }
                        combine(flows) { arrays -> arrays.flatMap { it } }
                    }
                }
                .collect { allMovies ->
                    val f1Movies = allMovies.filter { isF1Content(it.name) }
                    _state.update { it.copy(movies = f1Movies, isMoviesLoading = false) }
                }
        }
    }

    private fun loadSeries() {
        viewModelScope.launch {
            providerRepository.getProviders()
                .flatMapLatest { providers ->
                    if (providers.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val flows = providers.map { seriesRepository.getSeries(it.id) }
                        combine(flows) { arrays -> arrays.flatMap { it } }
                    }
                }
                .collect { allSeries ->
                    val f1Series = allSeries.filter { isF1Content(it.name) }
                    _state.update { it.copy(series = f1Series, isSeriesLoading = false) }
                }
        }
    }

    private fun startClockTick() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                val nowMs = System.currentTimeMillis()
                _state.update { current ->
                    val races = recomputeStatuses(current.allRaces, nowMs)
                    val currentOrNext = resolveCurrentOrNext(races, nowMs)
                    current.copy(nowMs = nowMs, allRaces = races, currentOrNextWeekend = currentOrNext)
                }
            }
        }
    }

    private fun recomputeStatuses(races: List<F1RaceWeekend>, nowMs: Long): List<F1RaceWeekend> =
        races.map { weekend ->
            weekend.copy(
                sessions = weekend.sessions.map { session ->
                    session.copy(
                        status = computeSessionStatus(session.startTimeUtcMs, session.type, nowMs)
                    )
                }
            )
        }

    private fun resolveCurrentOrNext(races: List<F1RaceWeekend>, nowMs: Long): F1RaceWeekend? {
        val live = races.firstOrNull { w -> w.sessions.any { it.status == com.streamvault.domain.model.F1SessionStatus.LIVE } }
        if (live != null) return live
        return races
            .filter { w -> w.sessions.any { it.startTimeUtcMs > nowMs } }
            .minByOrNull { w -> w.sessions.minOf { it.startTimeUtcMs } }
    }
}
