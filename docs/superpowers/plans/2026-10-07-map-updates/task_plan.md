# 3D map gestures and route planning

Historical initial plan: the More-menu task below was later superseded by a scrollable toolbar rail. Current usage is documented in README and 使用说明.txt.

- [x] Test and improve midpoint zoom, pointer continuation and drawing isolation.
- [x] Keep top view highlighted and preserve its angle when rotating.
- [x] Group low frequency tools under More with mobile layout checks.
- [x] Build road intersection graph, calculate shortest connected routes and render them.
- [x] Test route selection, swapped endpoints, unreachable cases, edited scene updates.
- [x] Run all geometry/interaction tests, browser QA and Android build.

Preserve the user's latest 31 default transforms and existing road data. Routes use the schematic road network, not GPS or verified building entrances.


## Follow-up work completed · 2026-10-07

- [x] Update default roads to the full 212-entry export and gate rotation to -75°.
- [x] Export building edits relative to the current defaults.
- [x] Smooth road joins and share rendering with the editor.
- [x] Add endpoint/segment snapping, road reset and bounded undo/redo.
- [x] Add connectivity diagnostics and layout status with backup restoration.
- [x] Publish preview APK, screenshots and download instructions.
- [x] Synchronize README, feature inventory, usage guide and checkpoint notes.

Remaining: commit all feature source changes for reproducible APK builds; perform complete device regression.
