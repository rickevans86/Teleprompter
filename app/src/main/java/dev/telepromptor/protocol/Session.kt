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

data class Motion(
    val state: Playback = Playback(),
    val anchorMs: Long = 0,
) {
    fun position(nowMs: Long): Double {
        val elapsedSeconds = (nowMs - anchorMs).coerceAtLeast(0) / 1000.0
        val advance = if (state.playing) elapsedSeconds * state.fractionsPerSecond else 0.0
        return (state.scrollOffset + advance).coerceIn(0.0, 1.0)
    }

    fun sync(
        incoming: Playback,
        nowMs: Long,
        oneWayMs: Long,
        force: Boolean = false,
    ): Motion {
        val expected = Motion(incoming, nowMs - oneWayMs).position(nowMs)
        val local = position(nowMs)
        val threshold = (incoming.fractionsPerSecond * 0.25).coerceIn(0.00003, DRIFT_THRESHOLD)
        val snap = force || state.playing != incoming.playing || kotlin.math.abs(expected - local) > threshold
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
    private data class Peer(
        var lastSeen: Long,
        var name: String? = null,
        var oneWayMs: Long = 0,
        var receivedState: Boolean = false,
        var watchdog: Job? = null,
    )

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
        peers.forEach { (id, peer) ->
            peer.watchdog?.cancel()
            transport.close(id, disconnectLine())
        }
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
        countdown =
            scope.launch {
                delay(seconds * 1000L)
                mutableView.value = view.value.copy(countdownUntilMs = 0)
                update { it.copy(playing = true, scrollOffset = if (it.scrollOffset >= 1) 0.0 else it.scrollOffset) }
            }
    }

    private fun event(event: LinkEvent) {
        val localRole = role
        if (localRole == null) {
            if (event is LinkEvent.Open) transport.close(event.peer)
            return
        }
        when (event) {
            is LinkEvent.Open -> open(event.peer, localRole)
            is LinkEvent.Closed -> {
                peers.remove(event.peer)?.watchdog?.cancel()
                publishPeers()
            }
            is LinkEvent.Line -> receive(event, localRole)
        }
    }

    private fun open(
        id: String,
        localRole: Role,
    ) {
        if (peers.size >= if (localRole == Role.HOST) 3 else 1) {
            transport.close(id, disconnectLine())
            return
        }
        val peer = Peer(now())
        peers[id] = peer
        peer.watchdog = scope.launch { expireWhenSilent(id, peer) }
        send(id, Message(type = Kind.HELLO, role = localRole, appVersion = "1.0", deviceName = name()))
    }

    private fun receive(
        event: LinkEvent.Line,
        localRole: Role,
    ) {
        val peer = peers[event.peer] ?: return
        val message = Wire.decode(event.value) ?: return
        if (peer.name == null && message.type != Kind.HELLO) return
        peer.lastSeen = now()
        when (message.type) {
            Kind.HELLO -> hello(event.peer, peer, message, localRole)
            Kind.PING -> send(event.peer, Message(type = Kind.PONG, token = message.token))
            Kind.PONG -> message.token?.let { peer.oneWayMs = ((now() - it) / 2).coerceIn(0, 500) }
            Kind.DISCONNECT -> {
                if (localRole == Role.CLIENT) mutableView.value = view.value.copy(removedByHost = true)
                transport.close(event.peer)
            }
            else -> if (localRole == Role.CLIENT) applyRemote(message, peer)
        }
    }

    private fun hello(
        id: String,
        peer: Peer,
        message: Message,
        localRole: Role,
    ) {
        if (peer.name != null) return
        if (message.role == null || message.role == localRole) {
            transport.close(id)
            return
        }
        peer.name = message.deviceName?.take(80)?.ifBlank { id } ?: id
        publishPeers()
        if (localRole == Role.HOST) sendSnapshot(id)
    }

    private fun sendSnapshot(id: String) {
        val messages =
            view.value.script
                ?.let(Wire::chunks)
                .orEmpty() +
                Message(type = Kind.STATE, state = snapshot())
        transport.send(id, messages.map(Wire::encode))
        val remaining = view.value.countdownUntilMs - now()
        if (remaining > 0) {
            send(id, Message(type = Kind.COUNTDOWN, seconds = ((remaining + 999) / 1000).toInt()))
        }
    }

    private fun applyRemote(
        message: Message,
        peer: Peer,
    ) {
        val current = view.value
        when (message.type) {
            Kind.SCRIPT ->
                message.script?.let(assembler::accept)?.let { script ->
                    if (current.script != script) peer.receivedState = false
                    mutableView.value = current.copy(script = script)
                }
            Kind.STATE ->
                message.state?.takeIf(Playback::valid)?.let {
                    mutableView.value =
                        current.copy(
                            motion = current.motion.sync(it, now(), peer.oneWayMs, !peer.receivedState),
                            countdownUntilMs = if (it.playing) 0 else current.countdownUntilMs,
                        )
                    peer.receivedState = true
                }
            Kind.SEEK ->
                message.scrollOffset?.takeIf { it in 0.0..1.0 }?.let {
                    mutableView.value = current.copy(motion = Motion(current.motion.state.copy(scrollOffset = it), now()))
                }
            Kind.COUNTDOWN ->
                message.seconds?.takeIf { it in 0..3 }?.let {
                    mutableView.value = current.copy(countdownUntilMs = if (it == 0) 0 else now() + it * 1000L)
                }
            else -> Unit
        }
    }

    private fun tick() {
        val time = now()
        peers.toMap().forEach { (id, peer) ->
            if (time - peer.lastSeen >= TIMEOUT_MS) {
                removeSilentPeer(id)
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

    private suspend fun expireWhenSilent(
        id: String,
        peer: Peer,
    ) {
        while (peers[id] === peer) {
            val remaining = TIMEOUT_MS - (now() - peer.lastSeen)
            if (remaining <= 0) {
                removeSilentPeer(id)
                return
            }
            delay(remaining)
        }
    }

    private fun removeSilentPeer(id: String) {
        peers.remove(id)?.watchdog?.cancel()
        transport.close(id)
        publishPeers()
    }

    private fun snapshot(): Playback {
        val motion = view.value.motion
        val time = now()
        return motion.state.copy(scrollOffset = motion.position(time), hostTimestampMs = time)
    }

    private fun publishPeers() {
        mutableView.value =
            view.value.copy(
                peers =
                    peers
                        .mapNotNull { (id, peer) ->
                            peer.name?.let { id to it }
                        }.toMap(),
            )
    }

    private fun disconnectLine() = Wire.encode(Message(type = Kind.DISCONNECT))

    private fun send(
        peer: String,
        message: Message,
    ) = transport.send(peer, listOf(Wire.encode(message)))

    private fun broadcast(messages: List<Message>) {
        val lines = messages.map(Wire::encode)
        view.value.peers.keys
            .forEach { transport.send(it, lines) }
    }
}

class RetryDelay {
    private var nextMs = 1_000L

    fun afterFailure(): Long {
        val delayMs = nextMs
        nextMs = (nextMs * 2).coerceAtMost(8_000)
        return delayMs
    }

    fun connected() {
        nextMs = 1_000
    }
}
