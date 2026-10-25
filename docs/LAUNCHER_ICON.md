# Replacing the Scroll Break launcher icon

The app uses an adaptive launcher icon on Android 8.0 and newer. The adaptive icon combines a foreground with a background and applies the launcher’s shape mask. Keep the resource names below so the existing manifest references continue to work.

## Generate new artwork in Android Studio

1. Prepare a transparent foreground image, preferably SVG or a 432 × 432 PNG. Keep the brake pedal or pause mark centered inside the adaptive icon safe area; leave the corners transparent because launchers crop them.
2. In Android Studio, right-click the `app` module and choose **New → Image Asset**.
3. Select **Launcher Icons (Adaptive and Legacy)**. Set the name to `ic_launcher`, choose the foreground artwork, then set the background to the desired solid color (the current brand blue is `#1E3A8A`).
4. Review the round and squircle previews. Adjust scaling until the full symbol remains visible with each mask, then finish generating the assets.
5. Replace the adaptive resources at `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/drawable/ic_launcher_foreground.xml`, and `app/src/main/res/drawable/ic_launcher_background.xml`. Keep the adaptive XML pointing to the foreground and background drawables.
6. Replace the legacy raster icons in `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, and `mipmap-xxxhdpi` with the generated `ic_launcher.png` files (48, 72, 96, 144, and 192 px respectively). These are retained for resource completeness; the app’s minimum Android version uses the adaptive icon.
7. Keep `android:icon` and `android:roundIcon` in `AndroidManifest.xml` set to `@mipmap/ic_launcher` (or update both if you deliberately choose another resource name).
8. Rebuild and inspect the icon on a device or emulator with at least two launcher masks. Confirm the pause/brake mark is centered and not clipped.

## Edit the current vector directly

For a simple pause mark, edit `ic_launcher_foreground.xml`; for a different background, edit `ic_launcher_background.xml`. The foreground vector uses a 108 × 108 viewport. Keep important artwork within the central safe area, and retain transparent space around the outer edge. Regenerate the density PNGs from the same source artwork so adaptive and legacy icons stay visually consistent.
