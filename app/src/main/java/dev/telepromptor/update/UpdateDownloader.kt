package dev.telepromptor.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 60_000

class UpdateDownloader
    @Inject
    constructor() {
        /** Downloads the APK into [dir] and returns it, or null if anything fails. Older downloads are removed first. */
        suspend fun download(
            dir: File,
            url: String,
            tag: String,
        ): File? =
            withContext(Dispatchers.IO) {
                if (!url.startsWith("https://")) return@withContext null
                try {
                    dir.mkdirs()
                    dir.listFiles()?.forEach { it.delete() }
                    val name = tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val partial = File(dir, "update-$name.apk.part")
                    val target = File(dir, "update-$name.apk")
                    val connection = URL(url).openConnection() as HttpURLConnection
                    try {
                        connection.connectTimeout = CONNECT_TIMEOUT_MS
                        connection.readTimeout = READ_TIMEOUT_MS
                        connection.instanceFollowRedirects = true
                        if (connection.responseCode !in 200..299) return@withContext null
                        connection.inputStream.use { input -> partial.outputStream().use { input.copyTo(it) } }
                    } finally {
                        connection.disconnect()
                    }
                    if (partial.length() > 0 && partial.renameTo(target)) target else null
                } catch (_: IOException) {
                    null
                }
            }
    }
