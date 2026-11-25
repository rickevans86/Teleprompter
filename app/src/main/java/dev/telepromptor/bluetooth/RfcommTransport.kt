package dev.telepromptor.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.telepromptor.protocol.MAX_WIRE_BYTES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private val APP_UUID = UUID.fromString("e68d9913-0044-4c17-810a-7652e57ca4cd")

@SuppressLint("MissingPermission")
@Singleton
class RfcommTransport
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : Transport {
        private data class Packet(
            val lines: List<String>,
            val closeAfter: Boolean = false,
        )

        private class Connection(
            val socket: BluetoothSocket,
        ) {
            val outbox = Channel<Packet>(64)
        }

        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val inbox = Channel<LinkEvent>(128)
        override val events = inbox.receiveAsFlow()
        private val lock = Any()
        private val links = mutableMapOf<String, Connection>()
        private var listener: BluetoothServerSocket? = null
        private var generation = 0
        private var nextLink = 0L
        private val mutableListening = MutableStateFlow("")
        val listening = mutableListening.asStateFlow()

        fun listen() {
            val (ticket, previous) =
                synchronized(lock) {
                    val old = listener
                    listener = null
                    ++generation to old
                }
            mutableListening.value = "Starting Bluetooth listener…"
            scope.launch {
                var server: BluetoothServerSocket? = null
                try {
                    runCatching { previous?.close() }
                    val active =
                        adapter?.listenUsingRfcommWithServiceRecord("Telepromptor", APP_UUID)
                            ?: throw IOException("Bluetooth unavailable")
                    server = active
                    synchronized(lock) {
                        if (ticket != generation) return@launch
                        listener = active
                        mutableListening.value = "Waiting for clients"
                    }
                    while (true) {
                        val socket = active.accept()
                        val accepted =
                            synchronized(lock) {
                                if (ticket != generation || links.size >= 3) false else attach(socket) != null
                            }
                        if (!accepted) runCatching { socket.close() }
                    }
                } catch (_: Exception) {
                    synchronized(lock) {
                        if (ticket == generation) {
                            mutableListening.value = "Bluetooth listener stopped. Restart hosting."
                        }
                    }
                } finally {
                    runCatching { server?.close() }
                }
            }
        }

        fun stopListening() {
            val previous =
                synchronized(lock) {
                    generation++
                    mutableListening.value = ""
                    listener.also { listener = null }
                }
            scope.launch { runCatching { previous?.close() } }
        }

        suspend fun connect(address: String): String? =
            suspendCancellableCoroutine { result ->
                val job =
                    scope.launch {
                        var pending: BluetoothSocket? = null
                        try {
                            val radio = adapter ?: throw IOException("Bluetooth unavailable")
                            if (!result.isActive) return@launch
                            val socket = radio.getRemoteDevice(address).createRfcommSocketToServiceRecord(APP_UUID)
                            pending = socket
                            result.invokeOnCancellation { scope.launch { runCatching { socket.close() } } }
                            if (!result.isActive) return@launch
                            radio.cancelDiscovery()
                            socket.connect()
                            val peer = synchronized(lock) { if (result.isActive) attach(socket) else null }
                            if (peer != null) pending = null
                            if (result.isActive) result.resume(peer)
                        } catch (_: Exception) {
                            if (result.isActive) result.resume(null)
                        } finally {
                            runCatching { pending?.close() }
                        }
                    }
                result.invokeOnCancellation { job.cancel() }
            }

        private fun attach(socket: BluetoothSocket): String? {
            val address = socket.remoteDevice.address
            if (links.values.any { it.socket.remoteDevice.address == address }) return null
            val peer = "$address/${++nextLink}"
            val link = Connection(socket)
            links[peer] = link
            if (!inbox.trySend(LinkEvent.Open(peer)).isSuccess) {
                links.remove(peer)
                return null
            }
            scope.launch {
                try {
                    val input = BufferedInputStream(socket.inputStream)
                    val line = ByteArrayOutputStream()
                    while (true) {
                        val byte = input.read()
                        if (byte == -1) break
                        if (byte == 10) {
                            inbox.send(LinkEvent.Line(peer, line.toString(Charsets.UTF_8.name())))
                            line.reset()
                        } else {
                            if (line.size() >= MAX_WIRE_BYTES) throw IOException("Message too large")
                            line.write(byte)
                        }
                    }
                } catch (_: Exception) {
                    // Socket failure is reported through Closed.
                } finally {
                    finish(peer, link)
                }
            }
            scope.launch {
                try {
                    val output = socket.outputStream.buffered()
                    for (packet in link.outbox) {
                        packet.lines.forEach { output.write(it.encodeToByteArray()) }
                        output.flush()
                        if (packet.closeAfter) break
                    }
                } catch (_: Exception) {
                    // Socket failure is reported through Closed.
                } finally {
                    finish(peer, link)
                }
            }
            return peer
        }

        override fun send(
            peer: String,
            lines: List<String>,
        ) {
            val link = synchronized(lock) { links[peer] } ?: return
            if (!link.outbox.trySend(Packet(lines)).isSuccess) scope.launch { finish(peer, link) }
        }

        override fun close(
            peer: String,
            finalLine: String?,
        ) {
            val link = synchronized(lock) { links[peer] } ?: return
            if (finalLine == null || !link.outbox.trySend(Packet(listOf(finalLine), true)).isSuccess) {
                scope.launch { finish(peer, link) }
            }
        }

        private suspend fun finish(
            peer: String,
            link: Connection,
        ) {
            val removed =
                synchronized(lock) {
                    if (links[peer] !== link) {
                        false
                    } else {
                        links.remove(peer)
                        if (!inbox.trySend(LinkEvent.Closed(peer)).isSuccess) {
                            scope.launch { inbox.send(LinkEvent.Closed(peer)) }
                        }
                        true
                    }
                }
            if (!removed) return
            link.outbox.close()
            runCatching { link.socket.close() }
        }
    }
