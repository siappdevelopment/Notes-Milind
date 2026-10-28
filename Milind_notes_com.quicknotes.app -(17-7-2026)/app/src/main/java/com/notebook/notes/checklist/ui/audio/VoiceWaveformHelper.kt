package com.notebook.notes.checklist.ui.audio

import kotlin.math.sin
import kotlin.random.Random

object VoiceWaveformHelper {

    const val DEFAULT_BAR_COUNT = 48
    private const val MAX_AMPLITUDE = 32_767f
    private const val MIN_BAR_HEIGHT = 0.08f
    private const val AMPLITUDE_SENSITIVITY = 3.5f

    fun normalizeAmplitude(amplitude: Int): Float {
        val raw = amplitude / MAX_AMPLITUDE
        return (raw * AMPLITUDE_SENSITIVITY).coerceIn(MIN_BAR_HEIGHT, 1f)
    }

    fun downsample(samples: List<Float>, targetCount: Int = DEFAULT_BAR_COUNT): FloatArray {
        if (samples.isEmpty()) {
            return FloatArray(targetCount) { 0.08f }
        }
        if (samples.size <= targetCount) {
            return FloatArray(targetCount) { index ->
                val sampleIndex = (index * samples.size / targetCount.toFloat())
                    .toInt()
                    .coerceIn(0, samples.lastIndex)
                samples[sampleIndex].coerceIn(MIN_BAR_HEIGHT, 1f)
            }
        }

        val result = FloatArray(targetCount)
        val bucketSize = samples.size.toFloat() / targetCount
        for (index in 0 until targetCount) {
            val start = (index * bucketSize).toInt()
            val end = ((index + 1) * bucketSize).toInt().coerceAtMost(samples.size)
            var peak = 0f
            for (sampleIndex in start until end) {
                peak = maxOf(peak, samples[sampleIndex])
            }
            result[index] = peak.coerceIn(MIN_BAR_HEIGHT, 1f)
        }
        return result
    }

    fun fallbackWaveform(seed: String): FloatArray {
        val random = Random(seed.hashCode())
        return FloatArray(DEFAULT_BAR_COUNT) { index ->
            val centerDistance = (index - DEFAULT_BAR_COUNT / 2f) / (DEFAULT_BAR_COUNT / 2f)
            val envelope = 1f - (centerDistance * centerDistance * 0.45f)
            val wave = sin(index * 0.55f + random.nextFloat()) * 0.12f
            val noise = random.nextFloat() * 0.28f
            (0.12f + envelope * 0.42f + wave + noise).coerceIn(0.08f, 0.72f)
        }
    }
}
