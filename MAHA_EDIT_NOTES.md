# MAHA Garage revised project

## Implemented
- The application opens at `Vehicles` rather than the global dashboard, including after relaunch. First-time users see the existing empty-state/new vehicle CTA.
- The vehicle detail page uses a vertical arrangement on widths below 700dp and a 70:30 diagram / service overview split from 700dp onward (including expanded foldables).
- New interactive quick-service panel links to existing consumable editing dialogs. Original full service grid, tire management, and service history remain available below.
- The technical top-down car diagram was redrawn with vector paths: silhouette, glazing, hood, mirrors, four wheels, lamps, and component overlay locations. Size increases on larger screens.
- The vehicle selection cards have an upgraded dark gradient illustration surface while retaining distinct sedan/SUV/truck/van silhouettes and name/plate only.

## Verification and limitations
- Source changes were checked for consistent names and entry point. No installed Android SDK was available in this environment, so APK compilation and emulator/device testing could not be completed.
- Build via Android Studio: open this project, restore the local `google-services.json` under `app/` from your own Firebase project, sync Gradle, run `assembleDebug`, and test folded/unfolded layouts.
- `google-services.json`, `local.properties`, and generated caches/build outputs are intentionally excluded for safer sharing.
