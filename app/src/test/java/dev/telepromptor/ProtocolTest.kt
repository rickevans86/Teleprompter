package dev.telepromptor

import dev.telepromptor.bluetooth.LinkEvent
import dev.telepromptor.bluetooth.MemoryTransport
import dev.telepromptor.bluetooth.Transport
import dev.telepromptor.protocol.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ProtocolTest {
    @Test fun versionedAndDelimited() {
        val message = Message(type = Kind.STATE, state = Playback(mirror = true))
        assertTrue(Wire.encode(message).endsWith("\n"))
        assertEquals(message, Wire.decode(Wire.encode(message)))
        assertNull(Wire.decode(Wire.encode(message.copy(version = 2))))
        assertNull(Wire.decode("broken"))
    }

    @Test fun unicodeChunksAndChecksum() {
        val script = Script("id", "Title", "🏁\n".repeat(10_000))
        val chunks = Wire.chunks(script)
        val assembler = ScriptAssembler()
        assertTrue(chunks.size > 1)
        assertEquals(script, chunks.mapNotNull { assembler.accept(it.script!!) }.single())
        val corrupted = chunks.map { it.script!!.copy(checksum = "0".repeat(64)) }
        assertTrue(corrupted.mapNotNull(assembler::accept).isEmpty())
    }

    @Test fun suppressesSmallCorrectionsAndSnapsLargeOnes() {
        val current = Motion(Playback(playing = true, fractionsPerSecond = 0.1), 0)
        val small = current.state.copy(scrollOffset = 0.101)
        assertEquals(0.1, current.sync(small, 1000, 0).position(1000), 0.0001)
        assertEquals(0.4, current.sync(small.copy(scrollOffset = 0.4), 1000, 0).position(1000), 0.0001)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun fakeLinksSynchronizeAndReconnect() =
        runTest {
            val a = MemoryTransport("a")
            val b = MemoryTransport("b")
            val host = Session(a, backgroundScope, { testScheduler.currentTime }, { "Host" })
            val client = Session(b, backgroundScope, { testScheduler.currentTime }, { "Client" })
            host.begin(Role.HOST)
            client.begin(Role.CLIENT)
            val script = Script("one", "Title", "Script")
            host.script(script)
            a.connect(b)
            runCurrent()
            assertEquals(script, client.view.value.script)
            host.seek(0.6)
            runCurrent()
            assertEquals(0.6, client.view.value.motion.state.scrollOffset, 0.001)
            a.close("b")
            runCurrent()
            assertEquals(script, client.view.value.script)
            val retry = RetryDelay()
            assertEquals(listOf(1000L, 2000L, 4000L, 8000L, 8000L), List(5) { retry.afterFailure() })
            a.connect(b)
            runCurrent()
            assertEquals(mapOf("a" to "Host"), client.view.value.peers)
            retry.connected()
            assertEquals(1000L, retry.afterFailure())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun hostOnlyAcceptsThreePeers() =
        runTest {
            val radio = MemoryTransport("host")
            val host = Session(radio, backgroundScope, { testScheduler.currentTime }, { "Host" })
            host.begin(Role.HOST)
            repeat(4) { index ->
                val display = MemoryTransport("client-$index")
                val client = Session(display, backgroundScope, { testScheduler.currentTime }, { "Client $index" })
                client.begin(Role.CLIENT)
                radio.connect(display)
                runCurrent()
                assertEquals(
                    index < 3,
                    client.view.value.peers
                        .isNotEmpty(),
                )
            }
            assertEquals(3, host.view.value.peers.size)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun heartbeatClosesSilentPeer() =
        runTest {
            val sent = mutableListOf<Message>()
            val closed = mutableListOf<String>()
            val transport =
                object : Transport {
                    override val events = MutableSharedFlow<LinkEvent>(extraBufferCapacity = 4)

                    override fun send(
                        peer: String,
                        lines: List<String>,
                    ) {
                        sent += lines.mapNotNull(Wire::decode)
                    }

                    override fun close(
                        peer: String,
                        finalLine: String?,
                    ) {
                        closed += peer
                    }
                }
            val host = Session(transport, backgroundScope, { testScheduler.currentTime }, { "Host" })
            host.begin(Role.HOST)
            runCurrent()
            transport.events.tryEmit(LinkEvent.Open("silent"))
            transport.events.tryEmit(LinkEvent.Line("silent", Wire.encode(Message(type = Kind.HELLO, role = Role.CLIENT))))
            runCurrent()
            advanceTimeBy(6000)
            assertTrue(closed.isEmpty())
            runCurrent()
            assertEquals(2, sent.count { it.type == Kind.PING })
            assertEquals(listOf("silent"), closed)
            assertTrue(
                host.view.value.peers
                    .isEmpty(),
            )
        }
}
