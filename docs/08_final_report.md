# Project 2 Final Report: ScrollBrake — A Mindful Digital Wellness App

**Course / Project:** Project 2 Design a Digital Wellness App  
**Application Name:** ScrollBrake  
**Platform:** Android (Java / Kotlin / Jetpack Compose)  
**Target User:** Young adults, students, and professionals experiencing impulsive social media doomscrolling

---

## Executive Summary

**ScrollBrake** is an innovative Android digital wellness application designed to address mindless doomscrolling on short-form video and social media platforms (Instagram Reels, TikTok, YouTube Shorts). Unlike traditional screen-time managers that enforce rigid, easily-bypassed app lockouts, ScrollBrake introduces real-time, velocity-based **mindful speed bumps**. By detecting rapid, impulsive scroll gestures on-device and prompting a 5-second breathing pause with session intention input, ScrollBrake disrupts mindless habits and fosters conscious technology use without shaming the user.

---

## 1. User Research & Needs Analysis

User interviews with 4 target participants revealed three core insights:

1. **The Bypass Reflex:** Users habitually tap "Ignore limit for today" on standard smartphone digital wellbeing alerts.
2. **Impulsive Scroll Velocity:** Mindless doomscrolling is characterized by rapid, automatic swipe gestures.
3. **Guilt vs. Positive Reinforcement:** Dark red "time wasted" charts induce shame rather than behavioral change. Users prefer positive habit milestones and attention currency rewards.

---

## 2. Problem Statement & Market Gap

Standard app blockers treat digital wellbeing as a binary choice: complete app access or total lockout. This creates severe friction, causing users to disable blockers when they need an app for quick communication. ScrollBrake fills this gap by acting as a **mindful speed bump**—introducing micro-friction (5-second breathing pause + session intention prompt) at the exact moment mindless scrolling is detected.

---

## 3. Core System Architecture & Features

1. **On-Device Scroll Velocity Classification:** Implemented via real-time touch gesture monitoring and kinematic velocity classification algorithms to detect rapid swipe flings instantly.
2. **Mindful Speed Bump & Intention Prompt:** The speed bump overlay displays a 5-second breathing pause and prompts the user to state their intended task and duration.
3. **Study Focus Planner:** Allows users to schedule 25-minute focus sessions with 5-minute planned breaks and topic tags.
4. **Compassionate Analytics:** Rewards users with Attention Currency tokens and streak highlights for mindful choices on a non-shaming dashboard.

---

## 4. Usability Testing & Key Design Revisions

Testing with 6 consenting participants yielded an **overall 100% task completion rate**. Based on participant feedback, two key design revisions were made:

- **Revision 1 (Countdown Button Feedback):** Replaced static "Continue" buttons on the intervention overlay with a live disabled countdown state (`"Pause for 5s..."`) and breathing ring animation to prevent premature button taps.
- **Revision 2 (Onboarding Permission Explainer & Tooltips):** Added explicit pre-permission explainer screens explaining that zero keystrokes/data leave the device, and added helper tooltips in Focus Mode to clarify "Mindful Speed Bump" vs "Strict Lockout".

---

## 5. Privacy & Data Ethics Compliance

ScrollBrake operates under strict privacy-first principles:

- **100% Local On-Device Processing:** All scroll calculations and stats are stored in local encrypted SQLite databases.
- **Zero Data Transmission:** No user usage logs, intentions, or screen content are transmitted to external servers.

---

## Conclusion

ScrollBrake successfully demonstrates that micro-friction, intentional speed bumps, and positive reinforcement provide a significantly more effective and sustainable approach to digital wellness than punitive app lockouts.
