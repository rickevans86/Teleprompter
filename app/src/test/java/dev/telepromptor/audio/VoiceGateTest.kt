package dev.telepromptor.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceGateTest {
    @Test
    fun ignoresShortSoundsAndResumesOnSustainedSpeech() {
        val gate = VoiceGate()
        repeat(2) { assertFalse(gate.accept(800.0)) }
        assertFalse(gate.accept(30.0))
        repeat(2) { assertFalse(gate.accept(800.0)) }
        assertTrue(gate.accept(800.0))
    }

    @Test
    fun keepsScrollingThroughShortPausesThenStopsAndCanResume() {
        val gate = VoiceGate()
        repeat(3) { gate.accept(800.0) }
        repeat(24) { assertTrue(gate.accept(30.0)) }
        assertFalse(gate.accept(30.0))
        repeat(2) { assertFalse(gate.accept(800.0)) }
        assertTrue(gate.accept(800.0))
    }
}
