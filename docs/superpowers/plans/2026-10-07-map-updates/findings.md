# Findings

## Initial observations (before fixes)

The following observations record the initial investigation, rather than current defects.

- Roads use reference pixels at 4 pixels per scene unit. User hand drawn segments include crossings and short endpoint gaps.
- Current pinch zoom scales radius and approximates panning; it does not anchor to the touched ground position.
- Road editor currently edits on pointerdown before a second finger is recognized; multi-touch must roll back that unfinished gesture.
- Current top view uses phi=.025 but is not highlighted and rotation also changes phi.
- Current road and building objects can be updated locally; routes must use the current scene, not stale packaged transforms.

Implementation notes: graph uses world coordinates, splits intersections and obstacle boundary crossings, and joins endpoint gaps up to 2.1 scene units only where road widths overlap. Endpoint connectors are bounded to 35 scene units and checked against other building/lake polygons. Several hand drawn nearest branches are dead ends; route calculation considers nearby access alternatives and visibly renders access as dashed segments. Lake endpoints are placed on a nearby bank. Scene dimensions are schematic, so no real walking distance or time is displayed.


## Current implementation notes · 2026-10-07

- Default road data now contains 212 entries. Building and road defaults are captured before saved device layouts override runtime objects.
- Road rendering and editing previews share round joins and junction-clear markings; source road coordinates are preserved.
- Snapping targets nearby endpoints first, then segment projections. Erasing does not snap.
- Connectivity diagnostics report schematic geometric connections, not verified access through buildings. Marked endpoints can be legitimate entrances.
- Layout version metadata detects known updates; legacy saved data without a version is described as different from packaged defaults. Switching defaults retains one local backup.
- Building exports and readouts are default-relative; persisted transforms remain original-relative for compatibility.
- APK downloads and screenshots are available from README. Feature source changes are still partly uncommitted; source archives do not fully reproduce the published preview APK.
