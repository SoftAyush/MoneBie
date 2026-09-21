package com.aurex.app.security

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The app lock: whether it is on, the PIN behind it, and how long the app may
 * sit in the background before asking again.
 *
 * Kept outside the database, like the reminder setting, because it describes
 * this phone rather than the user's finances. A lock is a property of the
 * device the data sits on - a restored backup on a new phone should not arrive
 * locked behind a PIN that phone has never been told, and a wipe has to be able
 * to clear it along with everything else.
 *
 * The PIN is never stored. What is stored is a salted SHA-256 digest of it,
 * which is enough to check a guess and useless for recovering the digits. This
 * is a deterrent against a phone left on a table, not a vault: anyone with root
 * has the database itself.
 */
object AppLockSettings {

    private const val FILE = "aurex_app_lock"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_BIOMETRIC = "biometric"
    private const val KEY_TIMEOUT_SECONDS = "timeout_seconds"
    private const val KEY_PIN_LENGTH = "pin_length"

    const val PIN_MIN_LENGTH = 4
    const val PIN_MAX_LENGTH = 6

    /** How long the app may be away before the lock asks again. */
    enum class Timeout(val seconds: Long, val label: String) {
        IMMEDIATELY(0L, "Immediately"),
        ONE_MINUTE(60L, "After 1 minute"),
        FIVE_MINUTES(300L, "After 5 minutes"),
        FIFTEEN_MINUTES(900L, "After 15 minutes");

        companion object {
            fun of(seconds: Long): Timeout = entries.firstOrNull { it.seconds == seconds } ?: ONE_MINUTE
        }
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false) && hasPin(context)

    fun hasPin(context: Context): Boolean = prefs(context).getString(KEY_PIN_HASH, null) != null

    /**
     * How many digits the PIN has, so the gate can judge a guess the moment it
     * is complete rather than waiting for the longest allowed length. Reveals
     * the length, which every keypad lock does, and nothing about the digits.
     */
    fun pinLength(context: Context): Int =
        prefs(context).getInt(KEY_PIN_LENGTH, PIN_MIN_LENGTH).coerceIn(PIN_MIN_LENGTH, PIN_MAX_LENGTH)

    fun isBiometricEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun timeout(context: Context): Timeout =
        Timeout.of(prefs(context).getLong(KEY_TIMEOUT_SECONDS, Timeout.ONE_MINUTE.seconds))

    fun setTimeout(context: Context, timeout: Timeout) {
        prefs(context).edit().putLong(KEY_TIMEOUT_SECONDS, timeout.seconds).apply()
    }

    /** Sets (or replaces) the PIN and switches the lock on. */
    fun enable(context: Context, pin: String) {
        require(pin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH && pin.all(Char::isDigit)) {
            "PIN must be $PIN_MIN_LENGTH to $PIN_MAX_LENGTH digits"
        }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(context).edit()
            .putString(KEY_PIN_SALT, salt.toHex())
            .putString(KEY_PIN_HASH, digest(pin, salt).toHex())
            .putInt(KEY_PIN_LENGTH, pin.length)
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    /** Switches the lock off and forgets the PIN, so re-enabling asks for a new one. */
    fun disable(context: Context) {
        prefs(context).edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_LENGTH)
            .putBoolean(KEY_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC, false)
            .apply()
    }

    /** Whether [pin] is the one that was set. Always false when none is. */
    fun verify(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val storedHash = p.getString(KEY_PIN_HASH, null) ?: return false
        val salt = p.getString(KEY_PIN_SALT, null)?.fromHex() ?: return false
        return constantTimeEquals(digest(pin, salt).toHex(), storedHash)
    }

    /** Forgets everything, for a wipe back to a fresh install. */
    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
    }

    private fun digest(pin: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").run {
            update(salt)
            digest(pin.toByteArray(Charsets.UTF_8))
        }

    /** Compares without short-circuiting on the first differing character. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private fun String.fromHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
