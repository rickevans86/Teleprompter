package dev.telepromptor.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data object UpToDate : UpdateState

    data class Available(
        val tag: String,
        val apkUrl: String,
    ) : UpdateState

    data class NeedPermission(
        val tag: String,
        val apkUrl: String,
    ) : UpdateState

    data object Downloading : UpdateState

    data class Installing(
        val tag: String,
        val apkUrl: String,
    ) : UpdateState

    data class Failed(
        val reason: String,
    ) : UpdateState
}

interface UpdateActions {
    suspend fun check(): UpdateResult

    fun canInstall(): Boolean

    fun openInstallPermissionSettings()

    suspend fun download(
        tag: String,
        url: String,
    ): File?

    fun install(apk: File): Boolean
}

/** The "Check for updates" state machine. All Android work goes through [UpdateActions] so it can be tested. */
class UpdateFlow(
    private val actions: UpdateActions,
) {
    /** Silent check at startup: only a newer release changes anything. */
    suspend fun startupCheck(): UpdateState =
        when (val result = actions.check()) {
            is UpdateResult.Available -> UpdateState.Available(result.tag, result.apkUrl)
            else -> UpdateState.Idle
        }

    /** One tap on the update button. [onProgress] reports the busy states while the tap is being handled. */
    suspend fun tap(
        current: UpdateState,
        onProgress: (UpdateState) -> Unit = {},
    ): UpdateState =
        when (current) {
            is UpdateState.Available -> install(current.tag, current.apkUrl, onProgress)
            is UpdateState.NeedPermission -> install(current.tag, current.apkUrl, onProgress)
            is UpdateState.Installing -> install(current.tag, current.apkUrl, onProgress)
            UpdateState.Checking, UpdateState.Downloading -> current
            UpdateState.Idle, UpdateState.UpToDate, is UpdateState.Failed -> {
                onProgress(UpdateState.Checking)
                when (val result = actions.check()) {
                    is UpdateResult.Available -> UpdateState.Available(result.tag, result.apkUrl)
                    UpdateResult.UpToDate -> UpdateState.UpToDate
                    is UpdateResult.Failed -> UpdateState.Failed(result.reason)
                }
            }
        }

    private suspend fun install(
        tag: String,
        url: String,
        onProgress: (UpdateState) -> Unit,
    ): UpdateState {
        if (!actions.canInstall()) {
            actions.openInstallPermissionSettings()
            return UpdateState.NeedPermission(tag, url)
        }
        onProgress(UpdateState.Downloading)
        val apk = actions.download(tag, url) ?: return UpdateState.Failed("Download failed")
        return if (actions.install(apk)) {
            UpdateState.Installing(tag, url)
        } else {
            UpdateState.Failed("Could not open the Android installer")
        }
    }
}

class AndroidUpdateActions
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val checker: UpdateChecker,
        private val downloader: UpdateDownloader,
        private val installer: ApkInstaller,
    ) : UpdateActions {
        override suspend fun check() = checker.check()

        override fun canInstall() = installer.canInstall()

        override fun openInstallPermissionSettings() = installer.openInstallPermissionSettings()

        override suspend fun download(
            tag: String,
            url: String,
        ) = downloader.download(File(context.cacheDir, "updates"), url, tag)

        override fun install(apk: File) = installer.install(apk)
    }
