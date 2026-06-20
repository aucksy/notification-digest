package com.notdigest.app.core.util

/**
 * The *only* assumptions the app makes at first run. Everything else defaults to Digest.
 *
 * Per the product principle, we keep a tiny, obviously-critical set Real-Time by default —
 * SMS/messaging, authenticators/OTP, the dialer and alarms — and make no guesses about
 * banking, delivery, shopping, social, etc. The user owns every other decision.
 */
object CriticalDefaults {

    private val packages: Set<String> = setOf(
        // SMS / messaging
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.messaging",
        "com.android.mms",
        "com.whatsapp",
        "com.whatsapp.w4b",
        // Dialer / telephony
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.server.telecom",
        // Authenticators / OTP
        "com.google.android.apps.authenticator2",
        "com.azure.authenticator",
        "com.duosecurity.duomobile",
        "com.authy.authy",
        "com.lastpass.authenticator",
        "org.fedorahosted.freeotp",
        "com.twofasapp",
        "com.beemdevelopment.aegis",
        "com.okta.android.auth",
        // Assistants / AI (time-sensitive replies the user asked to keep real-time)
        "com.openai.chatgpt",
        "com.anthropic.claude",
        "com.google.android.apps.bard",
        // Google core
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.adm",
        // Clock / alarms
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
    )

    private val keywordHints = listOf(
        "messaging", "messages", "authenticator", "dialer", "otp", "2fa", "mfa",
    )

    /** True when an app should remain Real-Time by default. */
    fun isCritical(packageName: String, appName: String): Boolean {
        if (packageName in packages) return true
        val haystack = (packageName + " " + appName).lowercase()
        return keywordHints.any { haystack.contains(it) }
    }
}
