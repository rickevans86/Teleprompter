package dev.telepromptor.audio

/** Smooths short changes so syllables and brief gaps do not toggle playback. */
internal class VoiceGate {
    private var noiseLevel = 60.0
    private var loudFrames = 0
    private var quietFrames = 0
    private var active = false

    fun accept(level: Double): Boolean {
        val loud = level >= maxOf(150.0, noiseLevel * 2.7)
        if (loud) {
            quietFrames = 0
            loudFrames = (loudFrames + 1).coerceAtMost(3)
            if (loudFrames == 3) active = true
        } else {
            loudFrames = 0
            quietFrames = (quietFrames + 1).coerceAtMost(25)
            if (quietFrames == 25) active = false
            noiseLevel += (level - noiseLevel) * 0.04
        }
        return active
    }
}
