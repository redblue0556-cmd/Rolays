package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val META_PREFS_NAME = "rolays_appcheck_meta"
private const val META_INJECTED_DEBUG_KEY = "has_injected_debug_token"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val debugPrefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            val metaPrefs = context.getSharedPreferences(META_PREFS_NAME, Context.MODE_PRIVATE)

            if (!debugToken.isNullOrEmpty()) {
                // Synchronous commit() ensures the secret is written before DebugAppCheckProviderFactory reads it.
                debugPrefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                metaPrefs.edit()
                    .putBoolean(META_INJECTED_DEBUG_KEY, true)
                    .commit()
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "App Check debug secret injected and DebugAppCheckProviderFactory installed.")
            } else if (metaPrefs.getBoolean(META_INJECTED_DEBUG_KEY, false) &&
                !debugPrefs.getString(DEBUG_SECRET_KEY, null).isNullOrEmpty()
            ) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "Previously injected App Check debug token active.")
            } else {
                // On real phones running a debug APK from CI/GitHub without an injected debug token,
                // prefer reCAPTCHA Enterprise attestation so Firebase AI does not reject with 403.
                if (!installRecaptchaProvider(context)) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    Log.w(TAG, "No debug token or recaptcha_site_key found; fell back to DebugAppCheckProviderFactory.")
                }
            }
        } else {
            installRecaptchaProvider(context)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

private fun installRecaptchaProvider(context: Context): Boolean {
    val resId = context.resources.getIdentifier(
        RECAPTCHA_SITE_KEY_RES,
        "string",
        context.packageName
    )
    if (resId == 0) {
        Log.e(TAG, "recaptcha_site_key string resource not found in package.")
        return false
    }
    val siteKey = context.getString(resId)
    if (siteKey.isBlank()) {
        Log.e(TAG, "recaptcha_site_key resource string is blank.")
        return false
    }
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
    )
    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
    return true
}
