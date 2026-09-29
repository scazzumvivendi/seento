package com.scazzumvivendi.seento

import android.Manifest
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.scazzumvivendi.seento.ui.playlist.PlaylistLibraryScreen
import com.scazzumvivendi.seento.ui.playlist.PlaylistViewModel
import com.scazzumvivendi.seento.ui.playlist.PlaylistViewModelFactory
import com.scazzumvivendi.seento.ui.theme.SeentoTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.scazzumvivendi.seento.ui.playlist.PlaylistDetailScreen
import com.scazzumvivendi.seento.ui.wearable.WearableScreen
import com.scazzumvivendi.seento.ui.wearable.WearableViewModel
import com.scazzumvivendi.seento.ui.wearable.WearableViewModelFactory
import com.scazzumvivendi.seento.ui.wearable.DeviceTracksScreen
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.domain.model.Track
import com.scazzumvivendi.seento.R

private enum class AppScreen {
    PLAYLISTS,
    DEVICE,
    TRACKS,
    PLAYLIST_DETAIL,
    REMOTE_PLAYLIST_DETAIL,
    SETTINGS
}

private data class AppDestination(
    val screen: AppScreen,
    val playlistId: Long? = null
)

private val appDestinationSaver = listSaver<AppDestination, Any?>(
    save = { destination -> listOf(destination.screen.name, destination.playlistId) },
    restore = { saved ->
        AppDestination(
            screen = AppScreen.valueOf(saved[0] as String),
            playlistId = saved[1] as Long?
        )
    }
)

class MainActivity : AppCompatActivity() {

    private val viewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(
            repository = (application as SeentoApplication).playlistRepository,
            importPlaylistUseCase = (application as SeentoApplication)
                .importPlaylistUseCase,
            context = applicationContext
        )
    }

    private val wearableViewModel: WearableViewModel by viewModels {
        WearableViewModelFactory(applicationContext)
    }

    private var pendingScan: (() -> Unit)? = null

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            pendingScan?.invoke()
        } else {
            wearableViewModel.reportError(
                getString(R.string.bluetooth_permission_required)
            )
        }
        pendingScan = null
    }

    private val playlistPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    try {
                        val content = withContext(Dispatchers.IO) {
                            contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                                ?: throw IllegalStateException(
                                    getString(R.string.cannot_read_file)
                                )
                        }

                        val displayName = contentResolver.query(
                            uri,
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                            } else null
                        }
                        val fileName = displayName
                            ?.substringAfterLast('/')
                            ?.substringBeforeLast('.')
                            ?.takeIf { it.isNotBlank() }
                            ?: uri.lastPathSegment
                            ?.substringAfterLast('/')
                            ?.substringBeforeLast('.')
                            ?.ifBlank { null }
                            ?: getString(R.string.imported_playlist)

                        viewModel.importPlaylist(
                            content = content,
                            playlistName = fileName
                        )
                    } catch (exception: Exception) {
                        viewModel.reportImportError(
                            exception.message
                                ?: getString(R.string.cannot_read_file)
                        )
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {

            var destination by rememberSaveable(stateSaver = appDestinationSaver) {
                mutableStateOf(AppDestination(AppScreen.PLAYLISTS))
            }

            BackHandler(enabled = destination.screen != AppScreen.PLAYLISTS) {
                destination = when (destination.screen) {
                    AppScreen.PLAYLIST_DETAIL -> AppDestination(AppScreen.PLAYLISTS)
                    AppScreen.TRACKS -> AppDestination(AppScreen.DEVICE)
                    AppScreen.REMOTE_PLAYLIST_DETAIL -> AppDestination(AppScreen.DEVICE)
                    AppScreen.SETTINGS -> AppDestination(AppScreen.PLAYLISTS)
                    AppScreen.DEVICE -> destination.playlistId
                        ?.let { AppDestination(AppScreen.PLAYLIST_DETAIL, it) }
                        ?: AppDestination(AppScreen.PLAYLISTS)
                    AppScreen.PLAYLISTS -> destination
                }
            }

            SeentoTheme {
                val currentLanguage = androidx.compose.ui.platform.LocalConfiguration.current
                    .locales[0]?.language
                val state = viewModel.uiState
                    .collectAsStateWithLifecycle()
                    .value
                val wearableState = wearableViewModel.uiState
                    .collectAsStateWithLifecycle()
                    .value

                if (destination.screen == AppScreen.SETTINGS) {
                    com.scazzumvivendi.seento.ui.settings.SettingsScreen(
                        selectedLanguage = currentLanguage?.takeIf { it == "it" } ?: "en",
                        onLanguageSelected = { languageTag ->
                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                androidx.core.os.LocaleListCompat.forLanguageTags(languageTag)
                            )
                        },
                        onHomeClick = { destination = AppDestination(AppScreen.PLAYLISTS) },
                        onDeviceClick = { destination = AppDestination(AppScreen.DEVICE) },
                        onSettingsClick = { destination = AppDestination(AppScreen.SETTINGS) },
                        isDeviceConnected = wearableState.connectionStatus == com.scazzumvivendi.seento.data.ble.MdsConnectionStatus.CONNECTED
                    )
                } else if (destination.screen == AppScreen.TRACKS) {
                    DeviceTracksScreen(
                        state = wearableState,
                        onHomeClick = { destination = AppDestination(AppScreen.PLAYLISTS) },
                        onDeviceClick = { destination = AppDestination(AppScreen.DEVICE) },
                        onSettingsClick = { destination = AppDestination(AppScreen.SETTINGS) }
                    )
                } else if (destination.screen == AppScreen.DEVICE) {
                    WearableScreen(
                        state = wearableState,
                        onScanClick = ::startBluetoothScan,
                        onStopScanClick = wearableViewModel::stopScan,
                        onConnect = wearableViewModel::connect,
                        onDisconnect = wearableViewModel::disconnect,
                        onForceDisconnect = wearableViewModel::forceDisconnect,
                        onLoadMusic = wearableViewModel::loadMusic,
                        onTracksClick = { destination = AppDestination(AppScreen.TRACKS) },
                        onHomeClick = { destination = AppDestination(AppScreen.PLAYLISTS) },
                        localPlaylists = state.playlists,
                        onImportPlaylist = { remote, localPlaylistId ->
                            if (localPlaylistId == null) {
                                viewModel.importWatchPlaylist(remote, wearableState.tracks)
                            } else {
                                viewModel.keepWatchPlaylist(
                                    localPlaylistId,
                                    remote,
                                    wearableState.tracks
                                )
                            }
                        },
                        onCopyPlaylist = { remote ->
                            viewModel.copyWatchPlaylist(remote, wearableState.tracks)
                        },
                        onRemotePlaylistClick = { remote ->
                            destination = AppDestination(AppScreen.REMOTE_PLAYLIST_DETAIL, remote.id)
                        },
                        onSettingsClick = { destination = AppDestination(AppScreen.SETTINGS) }
                    )
                } else if (destination.screen == AppScreen.PLAYLISTS) {
                    PlaylistLibraryScreen(
                        state = state,
                        onImportClick = {
                            playlistPicker.launch(
                                arrayOf(
                                    "audio/x-mpegurl",
                                    "application/vnd.apple.mpegurl",
                                    "text/plain"
                                )
                            )
                        },
                        onCreatePlaylist = { name ->
                            viewModel.createPlaylist(name)
                        },
                        onRetry = viewModel::reload,
                        onWearableClick = { destination = AppDestination(AppScreen.DEVICE) },
                        onPlaylistClick = { playlistId ->
                            destination = AppDestination(AppScreen.PLAYLIST_DETAIL, playlistId)
                        },
                        onTracksClick = { destination = AppDestination(AppScreen.TRACKS) },
                        lastDeviceName = wearableState.lastDeviceName,
                        isDeviceConnected = wearableState.connectionStatus == com.scazzumvivendi.seento.data.ble.MdsConnectionStatus.CONNECTED,
                        deviceFeedback = wearableState.successMessage ?: wearableState.errorMessage,
                        deviceFeedbackIsError = wearableState.errorMessage != null,
                        onSendPlaylist = { playlist ->
                            if (wearableState.connectionStatus == com.scazzumvivendi.seento.data.ble.MdsConnectionStatus.CONNECTED) {
                                wearableViewModel.writePlaylist(playlist) { remoteId ->
                                    viewModel.setRemotePlaylistId(playlist.id, remoteId)
                                }
                            } else destination = AppDestination(AppScreen.DEVICE)
                        },
                        onSettingsClick = { destination = AppDestination(AppScreen.SETTINGS) }
                    )
                } else if (destination.screen == AppScreen.PLAYLIST_DETAIL) {
                    val selectedPlaylist = state.playlists
                        .firstOrNull { it.id == destination.playlistId }

                    if (selectedPlaylist != null) {
                        PlaylistDetailScreen(
                            playlist = selectedPlaylist,
                            onMoveTrack = { fromIndex, toIndex ->
                                viewModel.moveTrack(
                                    playlistId = selectedPlaylist.id,
                                    fromIndex = fromIndex,
                                    toIndex = toIndex
                                )
                            },
                            onSaveTrackOrder = {
                                viewModel.saveTrackOrder(selectedPlaylist.id)
                            },
                            onRemoveTrack = { index ->
                                viewModel.removeTrackAt(
                                    playlistId = selectedPlaylist.id,
                                    position = index
                                )
                            },
                            onRename = { newName ->
                                viewModel.renamePlaylist(
                                    playlistId = selectedPlaylist.id,
                                    newName = newName
                                )
                            },
                            onDelete = {
                                viewModel.deletePlaylist(selectedPlaylist.id)
                                destination = AppDestination(AppScreen.PLAYLISTS)
                            },
                            isSaving = state.isSaving,
                            errorMessage = state.errorMessage,
                            deviceTracks = wearableState.tracks,
                            onAddDeviceTracks = { tracks ->
                                viewModel.addDeviceTracks(selectedPlaylist.id, tracks)
                            },
                            onOpenDevice = {
                                destination = AppDestination(AppScreen.DEVICE, selectedPlaylist.id)
                            },
                            isDeviceConnected = wearableState.connectionStatus == com.scazzumvivendi.seento.data.ble.MdsConnectionStatus.CONNECTED,
                            isSendingPlaylist = wearableState.isWritingPlaylist,
                            sendFeedback = wearableState.successMessage ?: wearableState.errorMessage,
                            sendFeedbackIsError = wearableState.errorMessage != null,
                            onHomeClick = { destination = AppDestination(AppScreen.PLAYLISTS) },
                            onDeviceClick = {
                                destination = AppDestination(AppScreen.DEVICE)
                            },
                            onTracksClick = {
                                destination = AppDestination(AppScreen.TRACKS)
                            },
                            onSettingsClick = { destination = AppDestination(AppScreen.SETTINGS) },
                            onSendPlaylist = { playlist ->
                                if (wearableState.connectionStatus == com.scazzumvivendi.seento.data.ble.MdsConnectionStatus.CONNECTED) {
                                    wearableViewModel.writePlaylist(playlist) { remoteId ->
                                        viewModel.setRemotePlaylistId(playlist.id, remoteId)
                                    }
                                } else destination = AppDestination(AppScreen.DEVICE)
                            }
                        )
                    } else {
                        destination = AppDestination(AppScreen.PLAYLISTS)
                    }
                } else {
                    val remote = wearableState.remotePlaylists
                        .firstOrNull { it.id == destination.playlistId }
                    val tracksByKey = wearableState.tracks.associateBy { it.key }
                    val remoteDetail = remote?.let { playlist ->
                        Playlist(
                            id = -playlist.id,
                            name = playlist.name,
                            remotePlaylistId = playlist.id,
                            tracks = playlist.songKeys.mapNotNull { key ->
                                val track = tracksByKey[key] ?: return@mapNotNull null
                                Track(
                                    id = 0L,
                                    title = track.title,
                                    path = track.path,
                                    durationSeconds = track.durationMillis?.div(1_000L)?.toInt(),
                                    deviceKey = track.deviceKey
                                )
                            }
                        )
                    }

                    if (remoteDetail != null) {
                        PlaylistDetailScreen(
                            playlist = remoteDetail,
                            onMoveTrack = { _, _ -> },
                            onRemoveTrack = {},
                            onRename = {},
                            onDelete = {
                                wearableViewModel.removePlaylist(remote.id)
                                destination = AppDestination(AppScreen.DEVICE)
                            },
                            readOnly = true,
                            showRemoveAction = true,
                            selectedBottomTab = 1,
                            isDeviceConnected = wearableState.connectionStatus == com.scazzumvivendi.seento.data.ble.MdsConnectionStatus.CONNECTED,
                            onHomeClick = { destination = AppDestination(AppScreen.PLAYLISTS) },
                            onDeviceClick = { destination = AppDestination(AppScreen.DEVICE) },
                            onTracksClick = { destination = AppDestination(AppScreen.TRACKS) },
                            onSettingsClick = { destination = AppDestination(AppScreen.SETTINGS) }
                        )
                    } else {
                        destination = AppDestination(AppScreen.DEVICE)
                    }
                }
            }
        }
        startBluetoothScan()
    }

    private fun startBluetoothScan() {
        val missingPermissions = requiredBluetoothPermissions()
            .filterNot { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

        if (missingPermissions.isEmpty()) {
            wearableViewModel.startScan()
            return
        }

        pendingScan = { wearableViewModel.startScan() }
        bluetoothPermissionLauncher.launch(missingPermissions.toTypedArray())
    }

    private fun requiredBluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
