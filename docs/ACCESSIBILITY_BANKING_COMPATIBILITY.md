# Accessibility & Banking Application Compatibility Architecture

## 1. Executive Summary & Security Policy

ScrollBreak implements a least-privilege, privacy-first accessibility architecture designed to prevent any compatibility conflicts or security concerns with banking, UPI, payment, and authentication applications (such as Google Pay, PhonePe, Paytm, YONO, HDFC, Bitwarden, etc.).

### Strict Compliance Mandate
- **No Spoofing or Bypass:** ScrollBreak does not attempt to conceal its `AccessibilityService`, spoof system accessibility flags, or bypass financial applications' security checks.
- **Dynamic Event Isolation:** Monitored packages are dynamically restricted via `AccessibilityServiceInfo.setPackageNames()`. The Android OS does not deliver accessibility events to ScrollBreak when payment or banking applications are in use.
- **Centralized Sensitive Application Exclusion:** `SensitiveAppPolicy` provides a hardcoded and user-configurable exclusion list that halts event handling and node processing before any UI inspection occurs.

---

## 2. Security Issues Resolved

| Original Issue | Root Cause | Architectural Fix Implemented |
|---|---|---|
| **Broad Event Monitoring** | `android:accessibilityEventTypes="typeAllMask"` listened to events from all apps on the device | Restricted to `typeWindowStateChanged\|typeViewScrolled\|typeWindowContentChanged` |
| **Overly Broad Flags** | `flagIncludeNotImportantViews`, `flagRetrieveInteractiveWindows`, `flagRequestEnhancedWebAccessibility` | Removed unnecessary flags; retained only `flagReportViewIds` for target social apps/browsers |
| **No Package Filter** | `packageNames` was `null`, allowing event delivery for financial apps | Programmatically set `packageNames` via `updateDynamicPackageFilter()` to target social/distracting apps only |
| **Node Hierarchy Traversal on Sensitive Apps** | `onAccessibilityEvent` inspected node trees regardless of package | Integrated `SensitiveAppPolicy.isSensitiveApp()` early exit guard at top of `onAccessibilityEvent()` |
| **Overlay Contamination** | `BlockingOverlayDisplayActivity` could launch over any app | Added sensitive package check in `processIntent()` to finish overlays immediately if requested on sensitive apps |
| **Over-Reliance on Accessibility** | Ordinary screen time tracking required `AccessibilityService` | Implemented `UsageStatsTracker` & `AppOperationalModeManager` for **Level 1 (UsageStats-only) Mode** |

---

## 3. Architecture Overview

### Dual Operational Modes

1. **Level 1: Basic Digital Wellbeing Mode (UsageStats-Only)**
   - Operates using `UsageStatsManager` (`PACKAGE_USAGE_STATS`).
   - Does **NOT** require `AccessibilityService` to be enabled at all.
   - Provides app usage tracking, daily time limits, focus timers, and session stats.
   - **100% Banking Compatible** under all device policies.

2. **Level 2: Advanced Least-Privilege Mode (Accessibility + UsageStats)**
   - Uses `AccessibilityService` with minimal privileges (`flagReportViewIds`).
   - Dynamically updates `info.packageNames` via `setServiceInfo()`.
   - Enforces `SensitiveAppPolicy` early exit guards.
   - Provides scroll velocity tracking, doomscrolling interventions, and browser URL filtering.

---

## 4. Protected Applications List

The built-in `SensitiveAppPolicy` automatically excludes the following categories:

- **Payments & Wallets:** Google Pay (`com.google.android.apps.nfc.payment`, `com.google.android.apps.walletnfcrerel`), PhonePe (`com.phonepe.app`), Paytm (`net.one97.paytm`), BHIM (`in.org.npci.upiapp`), CRED (`com.cred.club`), Amazon Pay (`com.amazon.pm`).
- **Banking Applications:** SBI YONO (`com.sbi.lotusintouch`, `com.sba.yono`), HDFC (`com.hdfcbank.payzapp`, `com.snapwork.hdfc`), ICICI (`com.csam.icici.bank.imobile`), Axis (`com.axis.mobile`), Kotak (`com.msf.k811`), Bank of Baroda, PNB, Canara, Union Bank, IDFC, IndusInd, Federal, RBL.
- **Authenticators & Password Managers:** Bitwarden (`com.x8bit.bitwarden`), 1Password (`com.onepassword.android`), LastPass (`com.lastpass.authenticator`), Google Authenticator, Microsoft Authenticator, Authy.
- **System Security & Biometrics:** `com.android.systemui`, Package Installers, Settings, Biometric Prompt screens.

---

## 5. User Settings & Management

Users can view and configure banking protection settings via:
`Settings -> SECURITY & PROTECTION -> Banking & Sensitive Apps Protection`

Screenshots & UI State:
- Switch between **Level 1 (Basic)** and **Level 2 (Advanced)** modes.
- View real-time System Status (UsageStats permission & Accessibility filter state).
- Browse all protected packages.

---

## 6. Testing & Verification Results

- **Unit Tests:** `26 passed, 0 skipped, 0 failed` via `gradlew :app:testDebugUnitTest`.
- **Debug Build:** Successfully assembled via `gradlew :app:assembleDebug`.
- **Dumpsys Inspection:** `adb shell dumpsys accessibilityservice` confirms `packageNames` are strictly restricted to monitored target apps.
