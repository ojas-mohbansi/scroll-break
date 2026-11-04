# Design Revision Record (Before vs. After)

Based on findings from user testing with 6 participants, two major design decisions were revised to reduce user confusion, improve accessibility, and reinforce positive behavior.

---

## Revision 1: Timer Button State & Countdown Guidance on Intervention Overlay

### The Problem
During Usability Task 2, participants (e.g. P3) repeatedly tapped the **"Continue Mindfully"** button while the 5-second breathing countdown was still active, expecting immediate dismissal. This caused mild frustration when nothing happened.

### Design Change (Before vs. After)

| Aspect | Before Testing (v1.0) | After Revision (v2.0) |
| :--- | :--- | :--- |
| **Button State** | Static "Continue" button displayed immediately with no visible disabled indicator. | **Dynamic Countdown State:** Button is disabled (`alpha = 0.5`, `clickable = false`) and displays `"Pause for 5s..."` with live countdown progress. |
| **Visual Cue** | Circular timer text in center only. | **Pulsing Breathing Ring:** Glowing breathing ring animation around the timer that transitions to a full green checkmark (`"Ready to proceed!"`) at 0s. |
| **User Impact** | Users felt the app was un-responsive or frozen during countdown. | Users clearly understand the required 5s pause and wait comfortably before resuming. |

```
BEFORE REVISION (v1.0):
+------------------------------------------+
|  [ 5 ] Pause & Breath                    |
|                                          |
|  [ Continue Mindfully ] <-- Tapped early |
+------------------------------------------+

AFTER REVISION (v2.0):
+------------------------------------------+
|  [ 🟢 3s ] Deep Breathing Pause          |
|                                          |
|  [ Pause (3s remaining)... ] [DISABLED]  |
+------------------------------------------+
```

---

## Revision 2: Onboarding Permission Explainer & Focus Mode Tooltips

### The Problem
During Usability Tasks 1 & 3, participants (P2, P5) hesitated when asked to grant Android Accessibility permissions and when choosing between "Mindful Speed Bump" vs "Hard Lockout" in Focus Mode.

### Design Change (Before vs. After)

| Aspect | Before Testing (v1.0) | After Revision (v2.0) |
| :--- | :--- | :--- |
| **Permission Flow** | Directly launched Android System Accessibility settings page with no prior explanation. | **Pre-Permission Explainer Screen:** Added a dedicated dialog explaining *why* Accessibility is required (local scroll detection only) and reassuring users that *zero keystrokes or content are recorded*. |
| **Focus Mode Selection** | Radio buttons labeled "Mindful" and "Strict". | **Interactive Helper Cards:** Added subtitle tooltips: <br>• *Mindful:* "Shows 5s speed bump if you scroll." <br>• *Strict:* "Completely blocks target apps during focus." |
| **User Impact** | Security hesitation & mode confusion. | 100% completion rate without assistance on permission setup and mode selection. |

```
BEFORE REVISION (v1.0):
Focus Mode:
(•) Mindful    ( ) Strict

AFTER REVISION (v2.0):
Focus Mode:
(•) Mindful Speed Bump
    └ Gently interrupts rapid scrolling with a 5s breathing pause
( ) Strict Lockout
    └ Completely blocks app during active focus session
```
