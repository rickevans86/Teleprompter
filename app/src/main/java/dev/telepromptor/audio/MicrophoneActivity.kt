package dev.telepromptor.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

private const val SAMPLE_RATE = 16_000
private const val FRAME_SAMPLES = SAMPLE_RATE / 50

@SuppressLint("MissingPermission")
internal fun microphoneActivity() =
    flow {
        val minBuffer =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        check(minBuffer > 0)
        val recorder =
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, FRAME_SAMPLES * Short.SIZE_BYTES * 4),
            )
        try {
            check(recorder.state == AudioRecord.STATE_INITIALIZED)
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            val samples = ShortArray(FRAME_SAMPLES)
            val gate = VoiceGate()
            while (currentCoroutineContext().isActive) {
                val count = recorder.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                check(count >= 0)
                if (count == 0) continue
                var power = 0.0
                for (index in 0 until count) {
                    val sample = samples[index].toDouble()
                    power += sample * sample
                }
                emit(gate.accept(sqrt(power / count)))
            }
        } finally {
            recorder.release()
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
