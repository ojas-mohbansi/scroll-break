# System Requirements Specification

This document details the functional and non-functional requirements for the **ScrollBrake** digital
wellness system.

---

## 1. Functional Requirements (FR)

### FR-1: Real-Time Scroll Intervention & Speed Bumps

- **FR-1.1 (Scroll Velocity Classification):** The system shall monitor screen touch events via an
  Accessibility Background Service and classify scroll gestures into *Normal*, *Fast*, or *Mindless*
  scroll velocities using real-time kinematic velocity algorithms.
- **FR-1.2 (Mindful Intervention Overlay):** When mindless scrolling is detected on monitored target
  apps, the system shall display an intervention speed bump overlay requiring a 5-second breathing
  pause.
- **FR-1.3 (Session Intention Prompt):** The intervention overlay shall prompt the user to state
  their session intent and target duration before continuing app usage.

### FR-2: Focus Mode & Planned Study Sessions

- **FR-2.1 (Focus Timer & Schedule):** Users shall be able to start an immediate focus session or
  schedule recurring study focus windows using the Focus Session Planner.
- **FR-2.2 (Planned Breaks):** Focus sessions shall support scheduled break intervals (e.g., 25m
  focus / 5m break) with break completion notifications.
- **FR-2.3 (Strict vs. Mindful Lock Modes):** Users can choose between strict app blocking or
  flexible intervention speed bumps during focus sessions.

### FR-3: Positive Habits & Reflection Dashboard

- **FR-3.1 (Attention Currency & Gamification):** The system shall reward users with "Attention
  Currency" tokens for every successful intervention pause and completed focus session.
- **FR-3.2 (Non-Shaming Analytics):** The analytics and reflection dashboard shall present habit
  streaks, focus time, and interventions accepted using positive reinforcement visuals rather than
  punitive red alert charts.
- **FR-3.3 (Habit Tracking):** Users shall be able to create, track, and log daily digital wellness
  habits.

---

## 2. Non-Functional Requirements (NFR)

### NFR-1: Privacy & Data Security

- **NFR-1.1 (On-Device Local Processing):** All scroll velocity calculations, app usage statistics,
  and intention logs shall be processed and stored strictly on-device using local encrypted SQLite
  databases.
- **NFR-1.2 (Zero Analytics Telemetry):** The system shall not upload personal usage patterns,
  keystrokes, or screen content to external cloud servers.

### NFR-2: Performance & Resource Efficiency

- **NFR-2.1 (Intervention Latency):** The intervention overlay shall trigger within **<100ms** of
  mindless scroll velocity detection.
- **NFR-2.2 (Battery Overhead):** The background Accessibility Monitoring Service shall consume
  **<2%** total battery over a standard 24-hour cycle.

### NFR-3: Accessibility & Usability

- **NFR-3.1 (Color Contrast & Material Design):** UI components shall adhere to WCAG AA contrast
  guidelines and Material Design 3 guidelines.
- **NFR-3.2 (Clear Navigation):** Core task navigation (starting a focus session or reviewing
  analytics) shall be accessible within 2 taps from the home dashboard.
