package dev.nocturne.ambient

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nocturne.ambient.data.CalendarRepository
import dev.nocturne.ambient.data.LocationRepository
import dev.nocturne.ambient.data.NoteServer
import dev.nocturne.ambient.data.SettingsRepository
import dev.nocturne.ambient.data.WeatherRepository
import dev.nocturne.ambient.model.CalendarEvent
import dev.nocturne.ambient.model.Face
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val weatherRepo = WeatherRepository(application)
    private val calendarRepo = CalendarRepository()
    private val locationRepo = LocationRepository()

    val settings: StateFlow<Settings> = settingsRepo.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings()
    )

    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather.asStateFlow()

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    private val noteFile = File(application.filesDir, "note.png")
    private var noteServer: NoteServer? = null

    private val _note = MutableStateFlow<ByteArray?>(null)
    val note: StateFlow<ByteArray?> = _note.asStateFlow()

    private val _noteUpdatedAt = MutableStateFlow<Long?>(null)
    val noteUpdatedAt: StateFlow<Long?> = _noteUpdatedAt.asStateFlow()

    private val _noteUrl = MutableStateFlow<String?>(null)
    val noteUrl: StateFlow<String?> = _noteUrl.asStateFlow()

    private val _noteServerReady = MutableStateFlow(false)
    val noteServerReady: StateFlow<Boolean> = _noteServerReady.asStateFlow()

    private val _noteServerFailed = MutableStateFlow(false)
    val noteServerFailed: StateFlow<Boolean> = _noteServerFailed.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _hud = MutableStateFlow<String?>(null)
    val hud: StateFlow<String?> = _hud.asStateFlow()

    private val _exitArmed = MutableStateFlow(false)
    val exitArmed: StateFlow<Boolean> = _exitArmed.asStateFlow()

    private var hudJob: Job? = null
    private var exitJob: Job? = null
    private var detectedLocation = false

    init {
        viewModelScope.launch {
            val current = settingsRepo.current()
            if (current.locationAuto && !current.locationResolved) {
                launch {
                    val city = locationRepo.autoLocate()
                    if (city != null) {
                        detectedLocation = true
                        settingsRepo.update {
                            it.copy(
                                locationLabel = city.label,
                                latitude = city.latitude,
                                longitude = city.longitude,
                                locationResolved = true,
                                locationAuto = true
                            )
                        }
                    }
                }
            }

            launch(Dispatchers.IO) {
                if (noteFile.exists()) {
                    runCatching { noteFile.readBytes() }.getOrNull()?.let { bytes ->
                        _note.value = bytes
                        _noteUpdatedAt.value = noteFile.lastModified()
                    }
                }
            }

            launch {
                settings
                    .map { it.noteEnabled }
                    .distinctUntilChanged()
                    .collect { enabled ->
                        if (enabled) startNoteServer() else stopNoteServer()
                    }
            }

            launch {
                settings
                    .map { Triple(it.latitude, it.longitude, it.celsius) }
                    .distinctUntilChanged()
                    .collectLatest {
                        while (true) {
                            syncWeather(force = false)
                            delay(if (_weather.value == null) 65_000L else 15 * 60 * 1000L)
                        }
                    }
            }

            launch {
                settings
                    .map { it.calendarUrls }
                    .distinctUntilChanged()
                    .collectLatest {
                        while (true) {
                            syncCalendar(force = false)
                            delay(30 * 60 * 1000L)
                        }
                    }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _syncing.value = true
            syncWeather(force = true)
            syncCalendar(force = true)
            _syncing.value = false
            showHud("Updated")
        }
    }

    private suspend fun syncWeather(force: Boolean) {
        _weather.value = weatherRepo.get(settings.value, force = force)
    }

    private suspend fun syncCalendar(force: Boolean) {
        _events.value = calendarRepo.get(settings.value, force = force)
    }

    private suspend fun startNoteServer() {
        if (noteServer != null) return
        _noteServerFailed.value = false
        val server = NoteServer(noteFile) { bytes ->
            _note.value = bytes
            _noteUpdatedAt.value = if (bytes == null) null else System.currentTimeMillis()
            showHud(if (bytes == null) "Note cleared" else "Note updated")
        }
        val result = withContext(Dispatchers.IO) {
            val started = runCatching { server.startServer() }.isSuccess
            started to if (started) {
                NoteServer.localIp()?.let { "http://$it:${NoteServer.PORT}" }
            } else {
                null
            }
        }
        if (result.first) {
            noteServer = server
            _noteServerReady.value = true
            _noteUrl.value = result.second
        } else {
            _noteServerReady.value = false
            _noteUrl.value = null
            _noteServerFailed.value = true
        }
    }

    private suspend fun stopNoteServer() {
        val server = noteServer ?: return
        noteServer = null
        _noteServerReady.value = false
        _noteUrl.value = null
        _noteServerFailed.value = false
        withContext(Dispatchers.IO) { runCatching { server.stop() } }
    }

    fun clearNote() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { noteFile.delete() } }
            _note.value = null
            _noteUpdatedAt.value = null
            showHud("Note cleared")
        }
    }

    fun refreshNoteUrl() {
        if (noteServer == null) return
        viewModelScope.launch {
            val ip = withContext(Dispatchers.IO) { NoteServer.localIp() }
            _noteUrl.value = ip?.let { "http://$it:${NoteServer.PORT}" }
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
        if (show) {
            _hud.value = null
            _exitArmed.value = false
        }
    }

    fun toggleSettings() = setShowSettings(!_showSettings.value)

    fun setFace(face: Face) {
        updateSettings { it.copy(face = face) }
        showHud(face.title)
    }

    fun cycleFace(direction: Int) {
        setFace(settings.value.face.next(direction))
    }

    fun setBrightness(value: Float) {
        val clamped = value.coerceIn(0.2f, 1f)
        updateSettings { it.copy(brightness = clamped) }
        showHud("Brightness ${(clamped * 100).toInt()}%")
    }

    fun adjustBrightness(delta: Float) {
        setBrightness(settings.value.brightness + delta)
    }

    fun showHud(text: String, millis: Long = 1_800L) {
        _hud.value = text
        hudJob?.cancel()
        hudJob = viewModelScope.launch {
            delay(millis)
            _hud.value = null
        }
    }

    fun armExit(): Boolean {
        if (_exitArmed.value) {
            _exitArmed.value = false
            return true
        }
        _exitArmed.value = true
        showHud("Press back again to close Nocturne", 2_200L)
        exitJob?.cancel()
        exitJob = viewModelScope.launch {
            delay(2_200L)
            _exitArmed.value = false
        }
        return false
    }

    fun clearExit() {
        _exitArmed.value = false
        exitJob?.cancel()
    }

    fun applyCity(city: dev.nocturne.ambient.model.City) {
        updateSettings {
            it.copy(
                locationAuto = false,
                locationResolved = true,
                locationLabel = city.label,
                latitude = city.latitude,
                longitude = city.longitude
            )
        }
        showHud(city.label)
    }

    fun useAutoLocation() {
        updateSettings { it.copy(locationAuto = true, locationResolved = false) }
        viewModelScope.launch {
            val city = locationRepo.autoLocate()
            if (city != null) {
                updateSettings {
                    it.copy(
                        locationAuto = true,
                        locationResolved = true,
                        locationLabel = city.label,
                        latitude = city.latitude,
                        longitude = city.longitude
                    )
                }
                showHud(city.label)
            } else {
                showHud("Could not detect location")
            }
        }
    }

    override fun onCleared() {
        runCatching { noteServer?.stop() }
        noteServer = null
        super.onCleared()
    }
}
