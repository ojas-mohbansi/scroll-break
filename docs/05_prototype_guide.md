# Interactive Prototype & Demonstration Guide

This guide details how to build, run, and interactively demonstrate the **ScrollBrake** digital
wellness Android prototype.

---

## 1. Prototype Core Architecture & Screen Mapping

The ScrollBrake interactive prototype consists of native Android activities and Jetpack Compose
screens that demonstrate the complete user journey:

| User Flow Step                     | Screen / Component Name                | Description                                                                                |
|:-----------------------------------|:---------------------------------------|:-------------------------------------------------------------------------------------------|
| **Step 1: Onboarding**             | Onboarding Screen                      | Initial setup, granting Accessibility permissions, selecting target apps.                  |
| **Step 2: Home Dashboard**         | Home Dashboard Screen                  | Overview of streak, Attention Currency, active interventions, and navigation.              |
| **Step 3: Blocker Control**        | Blocker Control & App Selection Screen | Selecting target social media apps and setting scroll velocity sensitivity thresholds.     |
| **Step 4: Mindful Intervention**   | Mindful Intervention Speed Bump Overlay| Real-time overlay triggered when rapid scrolling is detected. Displays 5s breathing pause. |
| **Step 5: Session Intention**      | Session Intention Prompt Screen        | User types their conscious intent and selects a target session duration.                   |
| **Step 6: Focus Mode**             | Focus Session Planner                  | Study focus planner with planned break intervals and topic tag.                            |
| **Step 7: Analytics & Reflection** | Reflection & Analytics Dashboard       | Non-shaming habit analytics, Attention Currency progress, and mindful intervention rates.  |

---

## 2. Interactive Demonstration Instructions (Offline / Emulator)

### Prerequisites

- Android Studio Ladybug (or newer) with JDK 17+
- Android Virtual Device (AVD) running Android 10 (API 29) or higher, or a connected physical
  Android device.

### Running the Demonstration

1. **Build & Deploy:**
   Execute Gradle build task or deploy the application to your emulator or device.
2. **Grant Accessibility Permission:**
    - On first launch, navigate to **Settings $\rightarrow$ Accessibility $\rightarrow$ ScrollBrake
      Accessibility Service** and enable it.
    - This enables the Accessibility Monitoring Service to detect scroll events.
3. **Demonstrate Mindful Intervention (Core Task 1):**
    - Open a target app (e.g. Instagram, TikTok, or Chrome).
    - Perform rapid vertical swipes (fast scroll velocity).
    - Observe the 5-second breathing pause overlay.
    - Type an intention (e.g., *"Looking for lecture notes"*) and tap **Continue Mindfully**.
4. **Demonstrate Study Focus Session (Core Task 2):**
    - Open ScrollBrake and navigate to **Focus Mode**.
    - Set duration to 25 minutes with a 5-minute planned break.
    - Tap **Start Focus Session** and observe active focus monitoring.
5. **Demonstrate Positive Reflection Dashboard (Core Task 3):**
    - Navigate to **Analytics & Habits** tab.
    - Review earned **Attention Currency** tokens and intervention acceptance rate.
