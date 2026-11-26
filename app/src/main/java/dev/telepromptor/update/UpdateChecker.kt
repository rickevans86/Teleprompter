package dev.telepromptor.update

import dev.telepromptor.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

const val LATEST_RELEASE_PAGE = "https://github.com/rickevans86/Teleprompter/releases/latest"
private const val LATEST_RELEASE_API = "https://api.github.com/repos/rickevans86/Teleprompter/releases/latest"
private const val TIMEOUT_MS = 10_000

sealed interface UpdateResult {
    data class Available(
        val tag: String,
        val apkUrl: String,
    ) : UpdateResult

    data object UpToDate : UpdateResult

    data class Failed(
        val reason: String,
    ) : UpdateResult
}

@Serializable
private data class ReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url") val url: String = "",
)

@Serializable
private data class ReleaseInfo(
    @SerialName("tag_name") val tagName: String = "",
    val assets: List<ReleaseAsset> = emptyList(),
)

private val json = Json { ignoreUnknownKeys = true }

/** Release tags end in the build's versionCode (`v1.1.<versionCode>`). Returns it, or null if the tag has no such number. */
fun releaseVersionCode(tag: String): Int? = tag.trim().substringAfterLast('.').toIntOrNull()

/** Compares a GitHub "latest release" response with the installed versionCode. */
fun parseRelease(
    body: String,
    installedCode: Int,
): UpdateResult {
    val release =
        try {
            json.decodeFromString<ReleaseInfo>(body)
        } catch (_: IllegalArgumentException) {
            return UpdateResult.Failed("Unreadable release information")
        }
    val latest = releaseVersionCode(release.tagName) ?: return UpdateResult.Failed("Unrecognised release tag ${release.tagName}")
    if (latest <= installedCode) return UpdateResult.UpToDate
    val apk = release.assets.firstOrNull { it.name.endsWith(".apk") && it.url.startsWith("https://") }
    return if (apk == null) UpdateResult.Failed("The release has no APK") else UpdateResult.Available(release.tagName, apk.url)
}

class UpdateChecker
    @Inject
    constructor() {
        suspend fun check(installedCode: Int = BuildConfig.VERSION_CODE): UpdateResult =
            withContext(Dispatchers.IO) {
                try {
                    val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
                    try {
                        connection.connectTimeout = TIMEOUT_MS
                        connection.readTimeout = TIMEOUT_MS
                        connection.setRequestProperty("Accept", "application/vnd.github+json")
                        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                            UpdateResult.Failed("GitHub answered ${connection.responseCode}")
                        } else {
                            parseRelease(connection.inputStream.bufferedReader().use { it.readText() }, installedCode)
                        }
                    } finally {
                        connection.disconnect()
                    }
                } catch (e: IOException) {
                    UpdateResult.Failed(e.message ?: "Network error")
                }
            }
    }
