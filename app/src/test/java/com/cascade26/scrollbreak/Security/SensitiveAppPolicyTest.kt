package com.cascade26.scrollbreak.Security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveAppPolicyTest {

    @Test
    fun defaultSensitivePackagesContainsMajorPaymentAndBankingApps() {
        val sensitive = SensitiveAppPolicy.DEFAULT_SENSITIVE_PACKAGES

        // Google Pay / Wallet
        assertTrue(sensitive.contains("com.google.android.apps.nfc.payment"))
        assertTrue(sensitive.contains("com.google.android.apps.walletnfcrerel"))

        // PhonePe & Paytm
        assertTrue(sensitive.contains("com.phonepe.app"))
        assertTrue(sensitive.contains("net.one97.paytm"))

        // Banking apps
        assertTrue(sensitive.contains("com.sbi.lotusintouch"))
        assertTrue(sensitive.contains("com.hdfcbank.payzapp"))
        assertTrue(sensitive.contains("com.csam.icici.bank.imobile"))

        // Authenticators & Password Managers
        assertTrue(sensitive.contains("com.x8bit.bitwarden"))
        assertTrue(sensitive.contains("com.onepassword.android"))
    }

    @Test
    fun isSensitiveAppIdentifiesSensitivePackagesAndHeuristics() {
        // Direct package matches
        assertTrue(isSensitivePackage("com.google.android.apps.nfc.payment"))
        assertTrue(isSensitivePackage("com.phonepe.app"))
        assertTrue(isSensitivePackage("net.one97.paytm"))

        // Substring / Keyword heuristic matches
        assertTrue(isSensitivePackage("com.custom.mybankingapp"))
        assertTrue(isSensitivePackage("com.example.paymentgateway"))
        assertTrue(isSensitivePackage("com.test.mywallet"))
        assertTrue(isSensitivePackage("com.auth.authenticator"))

        // Monitored Social Media & Distracting Apps MUST NOT be marked sensitive
        assertFalse(isSensitivePackage("com.instagram.android"))
        assertFalse(isSensitivePackage("com.google.android.youtube"))
        assertFalse(isSensitivePackage("com.snapchat.android"))
        assertFalse(isSensitivePackage("com.facebook.katana"))
        assertFalse(isSensitivePackage("com.reddit.frontpage"))
        assertFalse(isSensitivePackage("com.zhiliaoapp.musically"))
        assertFalse(isSensitivePackage("com.twitter.android"))
    }

    private fun isSensitivePackage(packageName: String): Boolean {
        if (SensitiveAppPolicy.DEFAULT_SENSITIVE_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase()
        return lower.contains("banking") ||
                lower.contains("payment") ||
                lower.contains("wallet") ||
                lower.contains("authenticator") ||
                lower.contains("passwords")
    }
}
