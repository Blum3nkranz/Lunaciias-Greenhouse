# Validation — 1.0.0

2026-10-19, Minecraft 26.1.2 / Fabric Loader 0.19.5 / Fabric API
0.155.3+26.1.2 / Java 25, Windows.

## Offline regression

`./gradlew test build`: 10 tests passed, no failures or skips.

Coverage: JSON and website share-code round trips, soil editing and rotation,
large crop footprints, malformed input, preset names/copy/switch/persistence,
legacy targets, projection state classification, legacy LUS preset migration,
source preservation and no overwrite of an existing standalone library.

## Isolated native client

`./gradlew -I tools/smoke.gradle runClient -PsmokeAssets=<existing assets directory>`:
36 checks passed. The harness sets master sound to 0 before every launch and
does not change the normal game instance. It loads the packaged production JAR,
Fabric API and Hypixel Mod API, with **no LUS installed**.

Coverage: 57-crop catalog; six vanilla soil textures; palette grid; automatic
soil placement; right-click crop/soil removal; keyboard binding/cancel/clear;
named preset save/switch/reload; screen return; `/greenhouse` and `/gh`;
bound-key opening; local-world correct/wrong/missing projection; tall mutation
frames; ground footprints; stopping projection. Editor and world-projection
screenshots were inspected for layout and rendering.

The harness remains in `tools/smoke` and is not bundled in the release JAR.
No unrelated LUS implementations, local user configuration or test worlds are
included in this repository or release.

## Unverified boundaries

No Hypixel account login or live Garden session was performed. Real-server
crop recognition and location packets retain the source implementation but
are not established by the local fixture. Co-installation with every third-party
mod is not certified. Windows was tested; Linux and macOS were not launched.

The editor uses Minecraft's own font and GUI scale rather than LUS's global
font implementation. Chinese and English labels follow the game language.
