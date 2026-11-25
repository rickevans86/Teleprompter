package dev.telepromptor.bluetooth

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface LinkEvent {
    val peer: String
    data class Open(override val peer: String) : LinkEvent
    data class Line(override val peer: String, val value: String) : LinkEvent
    data class Closed(override val peer: String) : LinkEvent
}

interface Transport {
    val events: Flow<LinkEvent>
    fun send(peer: String, lines: List<String>)
    fun close(peer: String, finalLine: String? = null)
}

class MemoryTransport(private val name: String) : Transport {
    private val inbox = Channel<LinkEvent>(Channel.UNLIMITED)
    private val peers = mutableMapOf<String, MemoryTransport>()
    override val events = inbox.receiveAsFlow()

    fun connect(other: MemoryTransport) {
        peers[other.name] = other
        other.peers[name] = this
        inbox.trySend(LinkEvent.Open(other.name))
        other.inbox.trySend(LinkEvent.Open(name))
    }

    override fun send(peer: String, lines: List<String>) {
        val other = peers[peer] ?: return
        lines.forEach { other.inbox.trySend(LinkEvent.Line(name, it)) }
    }

    override fun close(peer: String, finalLine: String?) {
        if (finalLine != null) send(peer, listOf(finalLine))
        val other = peers.remove(peer) ?: return
        other.peers.remove(name)
        other.inbox.trySend(LinkEvent.Closed(name))
        inbox.trySend(LinkEvent.Closed(peer))
    }
}
