package com.cascade26.scrollbreak.Security

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralized policy for identifying sensitive applications (banking, payment, UPI, password managers,
 * authenticators, and system security boundaries) that ScrollBreak MUST NEVER inspect, monitor,
 * or display overlays over.
 */
class SensitiveAppPolicy private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_FILE_NAME = "sensitive_app_policy_prefs"
        const val PREF_USER_SENSITIVE_EXCLUSIONS = "user_sensitive_exclusions"

        @Volatile
        private var INSTANCE: SensitiveAppPolicy? = null

        fun getInstance(context: Context): SensitiveAppPolicy {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SensitiveAppPolicy(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Default built-in list of sensitive payment, banking, authenticator, and credential apps.
         */
        val DEFAULT_SENSITIVE_PACKAGES: Set<String> = setOf(
            // Payment & Wallet Apps
            "com.google.android.apps.nfc.payment",
            "com.google.android.apps.walletnfcrerel",
            "com.google.android.apps.wallet",
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.cred.club",
            "com.amazon.pm",

            // Banking Applications
            "com.sbi.lotusintouch",
            "com.sba.yono",
            "com.statebankofindia.org",
            "com.hdfcbank.payzapp",
            "com.snapwork.hdfc",
            "com.csam.icici.bank.imobile",
            "com.icicibank.pockets",
            "com.axis.mobile",
            "com.msf.k811",
            "com.bankofbaroda.mconnect",
            "com.pnb.one",
            "com.canarabank.ai1",
            "com.infrasofttech.uboi",
            "com.idfcfirstbank.gplay",
            "com.indusind.mobile",
            "com.fedmobile",
            "com.rblbank.mobank",

            // Authenticators & Password Managers
            "com.x8bit.bitwarden",
            "com.onepassword.android",
            "com.lastpass.authenticator",
            "com.lastpass.lpandroid",
            "com.dashlane",
            "com.google.android.apps.authenticator2",
            "com.azure.authenticator",
            "com.authy.authy",

            // System Security & Biometric Boundaries
            "com.android.systemui",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.android.settings",
            "com.android.keyguard"
        )
    }

    /**
     * Retrieves all effective sensitive packages (built-in defaults + user exclusions).
     */
    fun getAllSensitivePackages(): Set<String> {
        val userExclusions = getUserExclusions()
        val result = HashSet(DEFAULT_SENSITIVE_PACKAGES)
        result.addAll(userExclusions)
        return result
    }

    /**
     * Returns true if the package is designated as sensitive and must be completely ignored.
     */
    fun isSensitiveApp(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        val lowerPkg = packageName.lowercase()
        val allSensitive = getAllSensitivePackages()

        if (allSensitive.contains(packageName)) return true

        // Substring / keyword guards for unrecognized banking / UPI apps
        return lowerPkg.contains("banking") ||
                lowerPkg.contains("payment") ||
                lowerPkg.contains("wallet") ||
                lowerPkg.contains("authenticator") ||
                lowerPkg.contains("passwords")
    }

    /**
     * Gets user-added custom excluded package names.
     */
    fun getUserExclusions(): Set<String> {
        return prefs.getStringSet(PREF_USER_SENSITIVE_EXCLUSIONS, emptySet()) ?: emptySet()
    }

    /**
     * Adds a user-defined package exclusion.
     */
    fun addUserExclusion(packageName: String) {
        val current = HashSet(getUserExclusions())
        current.add(packageName)
        prefs.edit().putStringSet(PREF_USER_SENSITIVE_EXCLUSIONS, current).apply()
    }

    /**
     * Removes a user-defined package exclusion.
     */
    fun removeUserExclusion(packageName: String) {
        val current = HashSet(getUserExclusions())
        current.remove(packageName)
        prefs.edit().putStringSet(PREF_USER_SENSITIVE_EXCLUSIONS, current).apply()
    }
}


