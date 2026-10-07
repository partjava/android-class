# Findings

- Roads use reference pixels at 4 pixels per scene unit. User hand drawn segments include crossings and short endpoint gaps.
- Current pinch zoom scales radius and approximates panning; it does not anchor to the touched ground position.
- Road editor currently edits on pointerdown before a second finger is recognized; multi-touch must roll back that unfinished gesture.
- Current top view uses phi=.025 but is not highlighted and rotation also changes phi.
- Current road and building objects can be updated locally; routes must use the current scene, not stale packaged transforms.

Implementation notes: graph uses world coordinates, splits intersections and obstacle boundary crossings, and joins endpoint gaps up to 2.1 scene units only where road widths overlap. Endpoint connectors are bounded to 35 scene units and checked against other building/lake polygons. Several hand drawn nearest branches are dead ends; route calculation considers nearby access alternatives and visibly renders access as dashed segments. Lake endpoints are placed on a nearby bank. Scene dimensions are schematic, so no real walking distance or time is displayed.
