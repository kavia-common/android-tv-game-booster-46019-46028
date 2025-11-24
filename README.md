# Android TV Game Booster

A modern Android TV app with an Ocean Professional theme to display real-time system stats and quick actions that help improve gaming performance.

Key features:
- Header with CPU and Memory usage (GPU is stubbed with TODO).
- Action buttons: Boost Performance, Clean Memory, Optimize Network (currently placeholders with user feedback).
- D‑pad navigable grid of game tiles with clear focus states.
- TV‑friendly visuals: rounded corners, subtle shadows, and smooth focus scaling.

Run locally:
- Open the project folder: android-tv-game-booster-46019-46028/game_booster_frontend
- Build/Run using Android Studio or: ./gradlew assembleDebug
- Install the debug APK on an Android TV emulator or device.

Implementation details:
- CPU usage: Read from /proc/stat and compute deltas for utilization.
- Memory usage: ActivityManager.MemoryInfo.
- GPU usage: Not generally exposed via public Android APIs; left as "N/A".
  - TODO: Integrate vendor-specific APIs if available on the target device.
- Actions:
  - Boost: placeholder for process priority tuning, animation scale reduction, or background cleanup.
  - Clean Memory: placeholder for cache cleanup or background task management (use cautiously).
  - Optimize Network: placeholder for TrafficStats tagging, DNS prefetch, or QoS adjustments.

How to extend:
- Replace demoGames() list with a PackageManager query to list installed games or shortcuts.
- Implement app launch via package names or deep links in the GamesAdapter click action.
- Add real optimization logic to the action handlers (applyBoost/applyClean/applyOptimize).
- Consider adding a background service to maintain optimizations during gameplay.

Notes:
- No external services or API keys required.
- Focus states are handled with scale animations and accent outlines for TV remotes.
