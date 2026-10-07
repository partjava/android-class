# 3D map gestures and route planning

- [x] Test and improve midpoint zoom, pointer continuation and drawing isolation.
- [x] Keep top view highlighted and preserve its angle when rotating.
- [x] Group low frequency tools under More with mobile layout checks.
- [x] Build road intersection graph, calculate shortest connected routes and render them.
- [x] Test route selection, swapped endpoints, unreachable cases, edited scene updates.
- [x] Run all geometry/interaction tests, browser QA and Android build.

Preserve the user's latest 31 default transforms and existing road data. Routes use the schematic road network, not GPS or verified building entrances.
