package dev.telepromptor.protocol

import dev.telepromptor.bluetooth.LinkEvent
import dev.telepromptor.bluetooth.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val DRIFT_THRESHOLD = 0.012
const val HEARTBEAT_MS = 2_000L
const val TIMEOUT_MS = 6_000L

data class Motion(val state: Playback = Playback(), val anchorMs: Long = 0) {
    fun position(nowMs: Long): Double {
        val elapsedSeconds = (nowMs - anchorMs).coerceAtLeast(0) / 1000.0
        val advance = if (state.playing) elapsedSeconds * state.fractionsPerSecond else 0.0
        return (state.scrollOffset + advance).coerceIn(0.0, 1.0)
    }

    fun sync(incoming: Playback, nowMs: Long, oneWayMs: Long, force: Boolean = false): Motion {
        val expected = Motion(incoming, nowMs - oneWayMs).position(nowMs)
        val local = position(nowMs)
        val snap = force || state.playing != incoming.playing || kotlin.math.abs(expected - local) > DRIFT_THRESHOLD
        return Motion(incoming.copy(scrollOffset = if (snap) expected else local), nowMs)
    }
}

data class SessionView(
    val motion: Motion = Motion(),
    val script: Script? = null,
    val peers: Map<String, String> = emptyMap(),
    val countdownUntilMs: Long = 0,
    val removedByHost: Boolean = false,
)

/** Call on the dispatcher used by [scope]. Socket workers deliver events through Transport. */
class Session(
    private val transport: Transport,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val name: () -> String,
) {
    private data class Peer(var lastSeen: Long, var name: String? = null, var oneWayMs: Long = 0)
    private val peers = linkedMapOf<String, Peer>()
    private val assembler = ScriptAssembler()
    private val mutableView = MutableStateFlow(SessionView())
    val view = mutableView.asStateFlow()
    private var role: Role? = null
    private var countdown: Job? = null

    init {
        scope.launch { transport.events.collect(::event) }
        scope.launch {
            while (isActive) {
                delay(HEARTBEAT_MS)
                tick()
            }
        }
    }

    fun begin(mode: Role) {
        if (role == mode) return
        end()
        role = mode
    }

    fun endIf(mode: Role) {
        if (role == mode) end()
    }

    fun end() {
        role = null
        countdown?.cancel()
        peers.keys.toList().forEach { transport.close(it, disconnectLine()) }
        peers.clear()
        mutableView.value = SessionView()
    }

    fun disconnect(peer: String) = transport.close(peer, disconnectLine())

    fun script(value: Script) {
        if (role != Role.HOST) return
        val chunks = Wire.chunks(value)
        mutableView.value = view.value.copy(script = value)
        stopCountdown()
        broadcast(chunks)
        update { it.copy(playing = false, scrollOffset = 0.0, fractionsPerSecond = 0.0) }
    }

    fun update(change: (Playback) -> Playback) {
        if (role != Role.HOST) return
        val time = now()
        val current = view.value.motion
        val state = change(current.state.copy(scrollOffset = current.position(time), hostTimestampMs = time))
        mutableView.value = view.value.copy(motion = Motion(state, time))
        broadcast(listOf(Message(type = Kind.STATE, state = state)))
    }

    fun seek(fraction: Double) {
        if (role != Role.HOST) return
        val offset = fraction.coerceIn(0.0, 1.0)
        broadcast(listOf(Message(type = Kind.SEEK, scrollOffset = offset)))
        update { it.copy(scrollOffset = offset) }
    }

    fun stopCountdown() {
        countdown?.cancel()
        mutableView.value = view.value.copy(countdownUntilMs = 0)
        broadcast(listOf(Message(type = Kind.COUNTDOWN, seconds = 0)))
    }

    fun play(seconds: Int) {
        if (role != Role.HOST) return
        stopCountdown()
        if (seconds == 0) {
            update { it.copy(playing = true, scrollOffset = if (it.scrollOffset >= 1) 0.0 else it.scrollOffset) }
            return
        }
        mutableView.value = view.value.copy(countdownUntilMs = now() + seconds * 1000L)
        broadcast(listOf(Message(type = Kind.COUNTDOWN, seconds = seconds)))
        countdown = scope.launch {
            delay(seconds * 1000L)
            mutableView.value = view.value.copy(countdownUntilMs = 0)
            update { it.copy(playing = true, scrollOffset = if (it.scrollOffset >= 1) 0.0 else it.scrollOffset) }
        }
    }

    private fun event(event: LinkEvent) {
        val localRole = role ?: return
        when (event) {
            is LinkEvent.Open -> {
                if (peers.size >= if (localRole == Role.HOST) 3 else 1) {
                    transport.close(event.peer)
                    return
                }
                peers[event.peer] = Peer(now())
                send(event.peer, Message(type = Kind.HELLO, role = localRole, appVersion = "1.0", deviceName = name()))
            }
            is LinkEvent.Closed -> {
                peers.remove(event.peer)
                publishPeers()
            }
            is LinkEvent.Line -> {
                val peer = peers[event.peer] ?: return
                val message = Wire.decode(event.value) ?: return
                if (peer.name == null && message.type != Kind.HELLO) return
                peer.lastSeen = now()
                when (message.type) {
                    Kind.HELLO -> {
                        if (message.role == null || message.role == localRole) {
                            transport.close(event.peer)
                            return
                        }
                        peer.name = message.deviceName?.take(80)?.ifBlank { event.peer } ?: event.peer
                        publishPeers()
                        if (localRole == Role.HOST) {
                            val messages = view.value.script?.let(Wire::chunks).orEmpty() +
                                Message(type = Kind.STATE, state = snapshot())
                            transport.send(event.peer, messages.map(Wire::encode))
                            val remaining = view.value.countdownUntilMs - now()
                            if (remaining > 0) send(event.peer, Message(
                                type = Kind.COUNTDOWN, seconds = ((remaining + 999) / 1000).toInt(),
                            ))
                        }
                    }
                    Kind.PING -> send(event.peer, Message(type = Kind.PONG, token = message.token))
                    Kind.PONG -> message.token?.let { peer.oneWayMs = ((now() - it) / 2).coerceIn(0, 500) }
                    Kind.DISCONNECT -> {
                        if (localRole == Role.CLIENT) mutableView.value = view.value.copy(removedByHost = true)
                        transport.close(event.peer)
                    }
                    else -> if (localRole == Role.CLIENT) applyRemote(message, peer)
                }
            }
        }
    }

    private fun applyRemote(message: Message, peer: Peer) {
        val current = view.value
        when (message.type) {
            Kind.SCRIPT -> message.script?.let(assembler::accept)?.let { script ->
                mutableView.value = current.copy(script = script)
            }
            Kind.STATE -> message.state?.takeIf(Playback::valid)?.let {
                mutableView.value = current.copy(
                    motion = current.motion.sync(it, now(), peer.oneWayMs),
                    countdownUntilMs = if (it.playing) 0 else current.countdownUntilMs,
                )
            }
            Kind.SEEK -> message.scrollOffset?.takeIf { it in 0.0..1.0 }?.let {
                mutableView.value = current.copy(motion = Motion(current.motion.state.copy(scrollOffset = it), now()))
            }
            Kind.COUNTDOWN -> message.seconds?.takeIf { it in 0..3 }?.let {
                mutableView.value = current.copy(countdownUntilMs = if (it == 0) 0 else now() + it * 1000L)
            }
            else -> Unit
        }
    }

    private fun tick() {
        val time = now()
        peers.toMap().forEach { (id, peer) ->
            if (time - peer.lastSeen >= TIMEOUT_MS) {
                transport.close(id)
                peers.remove(id)
                publishPeers()
            } else if (peer.name != null) {
                send(id, Message(type = Kind.PING, token = time))
            }
        }
        if (role == Role.HOST) {
            val state = snapshot()
            if (state.playing && state.scrollOffset >= 1) {
                update { it.copy(playing = false, scrollOffset = 1.0) }
            } else if (state.playing) {
                broadcast(listOf(Message(type = Kind.STATE, state = state)))
            }
        }
    }

    private fun snapshot(): Playback {
        val motion = view.value.motion
        val time = now()
        return motion.state.copy(scrollOffset = motion.position(time), hostTimestampMs = time)
    }

    private fun publishPeers() {
        mutableView.value = view.value.copy(peers = peers.mapNotNull { (id, peer) ->
            peer.name?.let { id to it }
        }.toMap())
    }

    private fun disconnectLine() = Wire.encode(Message(type = Kind.DISCONNECT))

    private fun send(peer: String, message: Message) = transport.send(peer, listOf(Wire.encode(message)))

    private fun broadcast(messages: List<Message>) {
        val lines = messages.map(Wire::encode)
        view.value.peers.keys.forEach { transport.send(it, lines) }
    }
}

class RetryDelay {
    private var nextMs = 1_000L

    fun afterFailure(): Long {
        val delayMs = nextMs
        nextMs = (nextMs * 2).coerceAtMost(8_000)
        return delayMs
    }

    fun connected() { nextMs = 1_000 }
}
