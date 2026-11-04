# Scroll Break

Scroll Break is an Android digital-wellbeing app for building intentional phone habits. It combines focus sessions, app and site blocking, daily habits and tasks, usage statistics, and an offline Attention Currency shop.

## Features

- **Focus / Lock In:** run a focus timer and protect the session from selected distractions.
- **App and site blocker:** set friction, reminders, temporary limits, or stronger blocks for supported apps and sites.
- **Daily limits:** Temp Lock can enforce a combined daily screen-time limit across tracked apps, a per-app scroll limit, or both.
- **Scroll interventions:** an optional classifier can detect rapid scrolling in supported apps and offer a stopping prompt.
- **Habits, tasks, and stats:** track completions and review local progress.
- **Attention Currency and Shop:** earn currency through focus and completions, then unlock local cosmetic and convenience items.
- **Offline storage:** app settings and user progress are stored on the device using Room and Android preferences.

## Requirements

- Android Studio with the Android SDK platform and build tools required by the current project configuration (`compileSdk 37`).
- JDK 17.
- Android 9 (API 28) or newer.

## Build and run

Open the repository in Android Studio and let Gradle sync. From a terminal, build the debug APK:

```shell
./gradlew :app:assembleDebug
```

On Windows:

```powershell
./gradlew.bat :app:assembleDebug
```

Install and launch the `app` configuration from Android Studio, or install `app/build/outputs/apk/debug/app-debug.apk` on a connected device. Run the unit tests with `./gradlew :app:testDebugUnitTest`; connected instrumentation tests require an authorized Android device or emulator.

## Permissions and system access

Scroll Break requests system access only for features that need it. Grant optional access from the app’s setup or Settings screens.

| Access | Purpose |
| --- | --- |
| Accessibility service | Observe supported app/window and scrolling events, count usage, and show configured interventions. The service must be enabled by the user in Android Settings. |
| Display over other apps | Show blocking and intervention overlays above the app being limited. |
| Notifications | Show focus, reminder, and task notifications on Android versions that require runtime notification permission. |
| Exact alarms | Schedule focus timers, task reminders, and daily reset work where Android requires special alarm access. |
| Ignore battery optimization (optional) | Help the user-requested background focus and blocking service continue reliably. Android may still apply device-specific background restrictions. |
| Device Admin (optional) | Android requires the user to deactivate this admin before uninstalling Scroll Break. This does not prevent force-stop and is not equivalent to device-owner management. |
| Microphone (optional) | Used only by the Scream lock challenge. |

Accessibility and overlay access are powerful. Review the Android system permission screens and enable only the features you want to use. Scroll Break’s accessibility service uses app/package and view information to apply the configured tracking and blocker rules; it is not a general-purpose screen recorder.

## Privacy and data

Usage counters, settings, habits, tasks, progression, and session outcome records are stored locally on the device. The app does not require an account for these features. Android device backups and the app’s own backup/restore flow may copy local data if you enable or use them. Scroll Break does not claim that local storage is encrypted against device-level access; use Android’s device security and backup controls accordingly.

The scroll counter relies on app-specific accessibility event and view detectors. Current scroll counting is wired for supported Instagram, YouTube, and Snapchat surfaces; UI changes or app variants can affect detection. Screen-time totals cover the app packages tracked by the project, not every app installed on the device. Android background limits, permission changes, force-stop, and device restarts can also affect continuous tracking.

## Contributing

Issues and pull requests are welcome. Keep user data local by default, preserve existing preference/database compatibility, and verify Android permission behavior on a device or emulator when changing blocker, focus, or tracking flows. Run the relevant Gradle unit tests and build before submitting changes.

## 🙏 Acknowledgements & Open Source Credits

Scroll Brake proudly stands on the shoulders of the open-source Android community. We gratefully acknowledge the following projects and technologies:

### Google Open Source & Android Ecosystem
- **[Android Jetpack & Material Components](https://developer.android.com/jetpack)** — Modern UI components and architecture libraries. *(Apache License 2.0)*
- **[Room Persistence Library](https://developer.android.com/training/data-storage/room)** — SQLite ORM for 100% local persistence. *(Apache License 2.0)*
- **[Gson](https://github.com/google/gson)** — Java serialization/deserialization library. *(Apache License 2.0)*

### Community Libraries
- **[Lottie Android](https://github.com/Airbnb/lottie-android)** (Airbnb) — Vector animation rendering. *(Apache License 2.0)*
- **[MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)** (Philipp Jahoda) — Android chart and visualization library. *(Apache License 2.0)*
- **[Balloon Tooltips](https://github.com/skydoves/Balloon)** (Jaewoong Eum / skydoves) — Tooltip popup framework. *(Apache License 2.0)*
- **[ProgressView](https://github.com/skydoves/ProgressView)** (Jaewoong Eum / skydoves) — Animated progress bar library. *(Apache License 2.0)*
- **[CircularSeekBar](https://github.com/tankery/CircularSeekBar)** (Tankery Chen / tankery) — Circular seekbar control for Android. *(Apache License 2.0)*

---

## 📬 Contact & Support

- **Repository**: [https://github.com/ojas-mohbansi/Scroll-Brake](https://github.com/ojas-mohbansi/Scroll-Brake)
- **Support Email**: `ojasmohbansi+scrollbreak@gmail.com`


## License

See [LICENSE](LICENSE).
