package com.streamvault.app.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.app.BuildConfig
import com.streamvault.app.update.AppUpdateDownloadStatus
import com.streamvault.app.update.AppUpdateInstaller
import com.streamvault.app.update.GitHubReleaseInfo
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Surfaces the boot-time "Nueva versión disponible" dialog when the cached
 * release info indicates a newer versionCode than what's installed.
 *
 * Reuses the same caches and installer the Settings screen uses, so the user
 * sees the same data whether they discover the update via the dialog or via
 * Settings → App update.
 */
@HiltViewModel
class AppUpdateBootViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val appUpdateInstaller: AppUpdateInstaller
) : ViewModel() {

    data class State(
        val available: Boolean = false,
        val versionName: String? = null,
        val releaseNotes: String = "",
        val downloadUrl: String? = null,
        val releaseUrl: String = "",
        val versionCode: Int? = null,
        val downloadStatus: AppUpdateDownloadStatus = AppUpdateDownloadStatus.Idle,
        val errorMessage: String? = null
    )

    private val _dismissedThisSession = MutableStateFlow(false)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        combine(
            preferencesRepository.cachedAppUpdateVersionName,
            preferencesRepository.cachedAppUpdateVersionCode,
            preferencesRepository.cachedAppUpdateDownloadUrl,
            preferencesRepository.cachedAppUpdateReleaseUrl,
            preferencesRepository.cachedAppUpdateReleaseNotes,
            appUpdateInstaller.downloadState,
            _dismissedThisSession
        ) { values ->
            val versionName = values[0] as String?
            val versionCode = values[1] as Int?
            val downloadUrl = values[2] as String?
            val releaseUrl = values[3] as String? ?: ""
            val releaseNotes = (values[4] as String?).orEmpty()
            val download = values[5] as com.streamvault.app.update.AppUpdateDownloadState
            val dismissed = values[6] as Boolean

            val installed = BuildConfig.VERSION_CODE
            val isNewer = versionCode != null && versionCode > installed
            State(
                available = isNewer && !dismissed,
                versionName = versionName,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releaseUrl = releaseUrl,
                versionCode = versionCode,
                downloadStatus = download.status
            )
        }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    fun onLater() {
        _dismissedThisSession.value = true
    }

    fun onUpdateNow() {
        val snapshot = _state.value
        val downloadUrl = snapshot.downloadUrl ?: return
        val versionName = snapshot.versionName ?: return

        viewModelScope.launch {
            when (snapshot.downloadStatus) {
                AppUpdateDownloadStatus.Downloaded -> {
                    install()
                }
                AppUpdateDownloadStatus.Downloading -> {
                    // already downloading — no-op
                }
                else -> {
                    val release = GitHubReleaseInfo(
                        versionName = versionName,
                        versionCode = snapshot.versionCode,
                        releaseUrl = snapshot.releaseUrl.ifBlank { "https://tv.mollo-ai.com/apps/" },
                        downloadUrl = downloadUrl,
                        releaseNotes = snapshot.releaseNotes,
                        publishedAt = null
                    )
                    when (val r = appUpdateInstaller.startDownload(release)) {
                        is Result.Error -> _state.value = _state.value.copy(errorMessage = r.message)
                        else -> Unit
                    }
                }
            }
        }
    }

    private suspend fun install() {
        when (val r = appUpdateInstaller.installDownloadedUpdate(expectedSha256 = null)) {
            is Result.Error -> _state.value = _state.value.copy(errorMessage = r.message)
            else -> Unit
        }
    }

    /**
     * Called from the Composable whenever the download flips to Downloaded.
     * The dialog observes this and auto-launches the system installer so the
     * user doesn't have to tap "Install" twice.
     */
    fun autoInstallIfReady() {
        if (_state.value.downloadStatus == AppUpdateDownloadStatus.Downloaded) {
            viewModelScope.launch { install() }
        }
    }

    suspend fun ensureCacheLoaded() {
        // Force initial collect so dialog can decide synchronously on first
        // composition. No-op if combine has already emitted.
        preferencesRepository.cachedAppUpdateVersionCode.first()
    }
}
