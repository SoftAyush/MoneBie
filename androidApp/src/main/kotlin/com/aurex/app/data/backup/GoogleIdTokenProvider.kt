package com.aurex.app.data.backup

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Asks the user to pick a Google account and returns the ID token for it.
 *
 * Credential Manager rather than the old GoogleSignIn client: the latter is
 * deprecated, and Credential Manager is what shows the account sheet users now
 * expect - the same one that appears everywhere else on the phone.
 *
 * Kept apart from [FirestoreBackupStore] because the two need different things.
 * This needs an Activity, since it puts a sheet on screen; the store needs
 * nothing but the string this produces. Splitting them is what lets the store
 * be constructed once for the whole process while this is called from whatever
 * screen happens to be showing.
 */
object GoogleIdTokenProvider {

    /**
     * @return the ID token, or null when the user dismissed the sheet.
     *
     * Cancelling is a decision, not a failure - the caller should fall silent
     * rather than show an error for something the user did deliberately - so it
     * is a null rather than one more exception to tell apart from the real ones.
     *
     * Only a real dismissal lands here. A phone with no Google account on it
     * raises [NoCredentialException] instead, which is reported: verified on an
     * emulator with no account signed in, where Play Services logs the provider
     * session as CANCELED but Credential Manager still surfaces the specific
     * exception. The provider's status and the exception the caller sees are not
     * the same thing, so this cannot be judged from the log alone.
     */
    suspend fun requestIdToken(activity: Activity): String? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    // False so that a user with no Aurex account yet is still
                    // offered their accounts. Filtering to already-authorised
                    // ones would show an empty sheet to every new user, which
                    // reads as "Google sign-in is broken".
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(activity.webClientId())
                    .build()
            )
            .build()

        val response = try {
            CredentialManager.create(activity).getCredential(activity, request)
        } catch (cancelled: GetCredentialCancellationException) {
            return null
        } catch (none: NoCredentialException) {
            throw IllegalStateException(
                "No Google account is available on this phone. Add one in the " +
                    "phone's settings and try again.",
                none
            )
        }

        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google sign-in returned an unexpected credential")
        }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    /**
     * The OAuth web client id the google-services plugin generates from
     * google-services.json.
     *
     * Looked up by name rather than through `R.string.default_web_client_id`,
     * because the plugin only emits that string once an OAuth client exists in
     * the Firebase project - which needs the app's SHA-1 fingerprint registered.
     * Referring to it directly would mean this module could not be compiled at
     * all until that had been done in the console, turning a configuration step
     * into a broken build. This way the app builds and runs, and only Google
     * sign-in itself reports what is missing.
     */
    private fun Context.webClientId(): String {
        val id = resources.getIdentifier("default_web_client_id", "string", packageName)
        check(id != 0) {
            "Google sign-in is not configured yet. Add this app's SHA-1 fingerprint to the " +
                "Firebase project, download the updated google-services.json, and rebuild."
        }
        return getString(id)
    }
}
