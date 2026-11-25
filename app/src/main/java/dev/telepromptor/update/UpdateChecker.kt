package dev.telepromptor.update

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
const val LATEST_APK_URL = "$LATEST_RELEASE_PAGE/download/Telepromptor.apk"
private const val LATEST_RELEASE_API = "https://api.github.com/repos/rickevans86/Teleprompter/releases/latest"
private const val TIMEOUT_MS = 5_000

data class AvailableUpdate(
    val version: String,
)

@Serializable
private data class ReleaseInfo(
    @SerialName("tag_name") val tagName: String = "",
)

/** Parses a release tag such as `v1.2.0` into numeric parts, or returns null if it is not a plain version. */
fun parseVersion(text: String): List<Int>? {
    val core = text.trim().removePrefix("v").substringBefore('-').substringBefore('+')
    val numbers = core.split('.').map { it.toIntOrNull() }
    return if (numbers.any { it == null }) null else numbers.filterNotNull()
}

/** True only when [latest] is a valid version that is strictly greater than a valid [installed] version. */
fun isNewerVersion(
    installed: String,
    latest: String,
): Boolean {
    val current = parseVersion(installed)
    val candidate = parseVersion(latest)
    if (current == null || candidate == null) return false
    val differing =
        (0 until maxOf(current.size, candidate.size)).firstOrNull {
            current.getOrElse(it) { 0 } != candidate.getOrElse(it) { 0 }
        }
    return differing != null && candidate.getOrElse(differing) { 0 } > current.getOrElse(differing) { 0 }
}

class UpdateChecker
    @Inject
    constructor() {
        private val json = Json { ignoreUnknownKeys = true }

        /** Returns the newer release, or null if up to date or the check could not be completed. */
        suspend fun check(installedVersion: String): AvailableUpdate? =
            withContext(Dispatchers.IO) {
                try {
                    fetchLatestTag()
                        ?.takeIf { isNewerVersion(installedVersion, it) }
                        ?.let { AvailableUpdate(it.trim().removePrefix("v")) }
                } catch (_: IOException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }

        private fun fetchLatestTag(): String? {
            val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                return json.decodeFromString<ReleaseInfo>(body).tagName
            } finally {
                connection.disconnect()
            }
        }
    }
