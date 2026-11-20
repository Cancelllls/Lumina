package com.cancellls.lumina.ui.haptics

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.atomic.AtomicBoolean

enum class LuminaHaptic {
    Tick,
    Select,
    Confirm,
    Heavy
}

class LuminaHaptics internal constructor(context: Context) {
    private val appContext = context.applicationContext

    fun play(haptic: LuminaHaptic) {
        HapticDevice.of(appContext)?.play(haptic)
    }
}

@Composable
fun rememberLuminaHaptics(): LuminaHaptics {
    val context = LocalContext.current
    return remember(context) { LuminaHaptics(context) }
}

private enum class BeatKind { Tick, Click, LowTick }

private data class Beat(
    val kind: BeatKind,
    val scale: Float,
    val delayMs: Int = 0
)

private fun LuminaHaptic.beats(): List<Beat> = when (this) {
    LuminaHaptic.Tick -> listOf(Beat(BeatKind.LowTick, 0.45f))
    LuminaHaptic.Select -> listOf(Beat(BeatKind.Tick, 0.7f))
    LuminaHaptic.Confirm -> listOf(
        Beat(BeatKind.Tick, 0.65f),
        Beat(BeatKind.Click, 0.85f, delayMs = 36)
    )
    LuminaHaptic.Heavy -> listOf(Beat(BeatKind.Click, 1.0f))
}

private class HapticDevice(
    private val vibrator: Vibrator,
    private val canCompose: Boolean,
    private val canScaleAmplitude: Boolean,
    private val systemHapticsEnabled: () -> Boolean
) {
    private val effects: Map<LuminaHaptic, VibrationEffect> = LuminaHaptic.entries.associateWith { haptic ->
        buildEffect(haptic)
    }

    fun play(haptic: LuminaHaptic) {
        if (!systemHapticsEnabled()) return
        val effect = effects[haptic] ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                TouchVibration.send(vibrator, effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect)
            }
        }
    }

    private fun buildEffect(haptic: LuminaHaptic): VibrationEffect {
        if (canCompose && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Primitives.compose(haptic.beats())
        }
        val durationMs = when (haptic) {
            LuminaHaptic.Tick -> 10L
            LuminaHaptic.Select -> 14L
            LuminaHaptic.Confirm -> 22L
            LuminaHaptic.Heavy -> 28L
        }
        val amplitude = if (canScaleAmplitude) {
            when (haptic) {
                LuminaHaptic.Tick -> 45
                LuminaHaptic.Select -> 85
                LuminaHaptic.Confirm -> 135
                LuminaHaptic.Heavy -> 175
            }
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        return VibrationEffect.createOneShot(durationMs, amplitude)
    }

    companion object {
        @Volatile
        private var instance: HapticDevice? = null

        @Volatile
        private var initialized = false

        fun of(app: Context): HapticDevice? {
            if (!initialized) {
                synchronized(this) {
                    if (!initialized) {
                        instance = create(app)
                        initialized = true
                    }
                }
            }
            return instance
        }

        private fun create(app: Context): HapticDevice? {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                app.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Vibrator::class.java)
            }
            if (vibrator == null || !vibrator.hasVibrator()) return null

            val canCompose = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Primitives.supportedBy(vibrator)

            return HapticDevice(
                vibrator = vibrator,
                canCompose = canCompose,
                canScaleAmplitude = vibrator.hasAmplitudeControl(),
                systemHapticsEnabled = systemHapticsWatcher(app)
            )
        }

        @Suppress("DEPRECATION")
        private fun systemHapticsWatcher(app: Context): () -> Boolean {
            val resolver = app.contentResolver
            val uri = Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED)
            fun read(): Boolean = runCatching {
                Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
            }.getOrDefault(true)

            val enabled = AtomicBoolean(read())
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    enabled.set(read())
                }
            }
            runCatching { resolver.registerContentObserver(uri, false, observer) }
            return { enabled.get() }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private object Primitives {
    fun supportedBy(vibrator: Vibrator): Boolean = vibrator.areAllPrimitivesSupported(
        VibrationEffect.Composition.PRIMITIVE_TICK,
        VibrationEffect.Composition.PRIMITIVE_CLICK
    )

    fun compose(beats: List<Beat>): VibrationEffect {
        val composition = VibrationEffect.startComposition()
        beats.forEach { beat ->
            composition.addPrimitive(
                beat.kind.toPrimitive(),
                beat.scale,
                beat.delayMs
            )
        }
        return composition.compose()
    }

    private fun BeatKind.toPrimitive(): Int = when (this) {
        BeatKind.Tick -> VibrationEffect.Composition.PRIMITIVE_TICK
        BeatKind.Click -> VibrationEffect.Composition.PRIMITIVE_CLICK
        BeatKind.LowTick -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        } else {
            VibrationEffect.Composition.PRIMITIVE_TICK
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object TouchVibration {
    private val attributes: VibrationAttributes = VibrationAttributes.Builder()
        .setUsage(VibrationAttributes.USAGE_TOUCH)
        .build()

    fun send(vibrator: Vibrator, effect: VibrationEffect) {
        @Suppress("MissingPermission")
        vibrator.vibrate(effect, attributes)
    }
}
