package com.monebie.app.security

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Whether the app is currently locked.
 *
 * Process-wide state rather than activity state, so the lock survives the
 * activity being recreated and cannot be slipped past by a rotation. The
 * activity reports going to and coming from the background; this decides
 * whether that absence was long enough to ask for the PIN again.
 */
object AppLock {

    private val _isLocked = mutableStateOf(false)
    val isLocked: State<Boolean> get() = _isLocked

    /** Elapsed-realtime stamp of the last trip to the background, or null while foregrounded. */
    private var backgroundedAt: Long? = null

    /** Set just before MoneBie itself opens another app's screen; consumed by [onBackground]. */
    private var externalActivityExpected = false

    /** Whether the current absence is one MoneBie started. */
    private var awayOnExternalActivity = false

    /**
     * How long a trip MoneBie itself started - a file picker, a share sheet, a
     * sign-in - may last before the lock treats it as having left.
     *
     * Without this, "lock immediately" would ask for the PIN on the way back
     * from choosing a CSV, which reads as the app not trusting its own button.
     * Two minutes is long enough to find a file and short enough that a phone
     * left on the picker is still covered.
     */
    private const val EXTERNAL_ACTIVITY_GRACE_SECONDS = 120L

    /** Called once per process, before the first screen. Locks a cold start. */
    fun onProcessStart(context: Context) {
        _isLocked.value = AppLockSettings.isEnabled(context)
    }

    /**
     * Declares that the next stop is MoneBie handing off to another screen -
     * call immediately before launching a picker, chooser or sign-in.
     */
    fun expectExternalActivity() {
        externalActivityExpected = true
    }

    /**
     * The activity has left the screen.
     *
     * [changingConfigurations] excludes a rotation, which passes through stop
     * and start in a few milliseconds and must not count as leaving.
     */
    fun onBackground(changingConfigurations: Boolean) {
        if (changingConfigurations) return
        backgroundedAt = SystemClock.elapsedRealtime()
        awayOnExternalActivity = externalActivityExpected
        externalActivityExpected = false
    }

    /** The activity is back. Locks if the absence outlasted the timeout. */
    fun onForeground(context: Context) {
        val away = backgroundedAt ?: return
        val external = awayOnExternalActivity
        backgroundedAt = null
        awayOnExternalActivity = false
        if (!AppLockSettings.isEnabled(context)) return

        val elapsedSeconds = (SystemClock.elapsedRealtime() - away) / 1000L
        val allowance = AppLockSettings.timeout(context).seconds.let { timeout ->
            if (external) maxOf(timeout, EXTERNAL_ACTIVITY_GRACE_SECONDS) else timeout
        }
        if (elapsedSeconds >= allowance) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
    }

    /** Re-locks immediately, for the moment the lock is first switched on. */
    fun lock() {
        _isLocked.value = true
    }

    /** Whether this phone can offer a fingerprint or face as an alternative to the PIN. */
    fun canUseBiometrics(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
}
