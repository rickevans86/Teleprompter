package dev.telepromptor.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

const val MAX_SCRIPT_BYTES = 2_000_000
const val MAX_WIRE_BYTES = 48_000
private const val CHUNK_BYTES = 12_000
private const val MAX_CHUNKS = (MAX_SCRIPT_BYTES + CHUNK_BYTES - 1) / CHUNK_BYTES

@Serializable
enum class Kind { HELLO, SCRIPT, STATE, SEEK, COUNTDOWN, PING, PONG, DISCONNECT }

@Serializable
enum class Role { HOST, CLIENT }

@Serializable
data class Playback(
    val playing: Boolean = false,
    val speedPxPerSec: Float = 90f,
    val fontSizeSp: Float = 42f,
    val mirror: Boolean = false,
    val theme: String = "dark",
    val align: String = "center",
    val scrollOffset: Double = 0.0,
    val hostTimestampMs: Long = 0,
    val fractionsPerSecond: Double = 0.0,
) {
    fun valid(): Boolean =
        speedPxPerSec in 10f..400f &&
            fontSizeSp in 16f..100f &&
            theme in setOf("light", "dark") &&
            align in setOf("left", "center") &&
            scrollOffset in 0.0..1.0 &&
            fractionsPerSecond in 0.0..100.0
}

data class Script(
    val id: String,
    val title: String,
    val text: String,
)

@Serializable
data class ScriptChunk(
    val id: String,
    val title: String,
    val index: Int,
    val count: Int,
    val checksum: String,
    val bytes: String,
)

@Serializable
data class Message(
    val version: Int = 1,
    val type: Kind,
    val role: Role? = null,
    val appVersion: String? = null,
    val deviceName: String? = null,
    val script: ScriptChunk? = null,
    val state: Playback? = null,
    val scrollOffset: Double? = null,
    val seconds: Int? = null,
    val token: Long? = null,
)

object Wire {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun encode(message: Message): String = json.encodeToString(Message.serializer(), message) + "\n"

    fun decode(line: String): Message? {
        if (line.length > MAX_WIRE_BYTES) return null
        return runCatching {
            val message = json.decodeFromString(Message.serializer(), line)
            message.takeIf { it.version == 1 }
        }.getOrNull()
    }

    fun chunks(script: Script): List<Message> {
        val bytes = script.text.encodeToByteArray()
        require(bytes.size <= MAX_SCRIPT_BYTES && script.title.length <= 200 && script.id.length <= 128)
        val checksum = digest(bytes)
        val count = ((bytes.size + CHUNK_BYTES - 1) / CHUNK_BYTES).coerceAtLeast(1)
        return List(count) { index ->
            val part = bytes.copyOfRange(index * CHUNK_BYTES, minOf((index + 1) * CHUNK_BYTES, bytes.size))
            Message(
                type = Kind.SCRIPT,
                script =
                    ScriptChunk(
                        script.id,
                        script.title,
                        index,
                        count,
                        checksum,
                        java.util.Base64
                            .getEncoder()
                            .encodeToString(part),
                    ),
            )
        }
    }

    internal fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

class ScriptAssembler {
    private var header: ScriptChunk? = null
    private val buffer = java.io.ByteArrayOutputStream()
    private var next = 0

    @Suppress("ReturnCount")
    fun accept(chunk: ScriptChunk): Script? {
        if (chunk.index == 0) {
            header = chunk
            next = 0
            buffer.reset()
        }
        val first = header ?: return null
        if (chunk.index != next || !matchesHeader(chunk, first)) return null
        val bytes =
            runCatching {
                java.util.Base64
                    .getDecoder()
                    .decode(chunk.bytes)
            }.getOrNull() ?: return null
        if (buffer.size() + bytes.size > MAX_SCRIPT_BYTES) return null
        buffer.write(bytes)
        next++
        if (next != chunk.count) return null
        header = null
        val complete = buffer.toByteArray()
        buffer.reset()
        return if (Wire.digest(complete) == chunk.checksum) {
            Script(chunk.id, chunk.title, complete.decodeToString())
        } else {
            null
        }
    }

    private fun matchesHeader(
        chunk: ScriptChunk,
        first: ScriptChunk,
    ): Boolean {
        val validIndex = chunk.count in 1..MAX_CHUNKS && chunk.index < chunk.count
        val validMetadata = chunk.id.length <= 128 && chunk.title.length <= 200 && chunk.checksum.length == 64
        val sameScript = chunk.copy(index = 0, bytes = "") == first.copy(index = 0, bytes = "")
        return validIndex && validMetadata && sameScript && chunk.bytes.length <= CHUNK_BYTES * 4 / 3
    }
}
