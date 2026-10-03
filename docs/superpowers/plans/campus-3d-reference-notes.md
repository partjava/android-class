# Building reconstruction references

This revision replaces shared rectangular courtyard templates with individually described outer contours, inner contours, annex blocks and roof types in `campus-footprints.js`. Each model records its source image from `map_assets`.

- Teaching buildings 1–4: separate courtyard proportions and corner projections; building 1 includes the low courtyard block visible in its image.
- Laboratory buildings 1–2 and comprehensive buildings 1–2: independent outlines; comprehensive building 1 has a side annex and low courtyard block.
- Library: bent lakeside body, stepped terrace, glazing and flat roof; four aboveground floors, one basement recorded but not exposed as an extra aboveground floor.
- Gym: light central roof plane with broad sloping shoulders.
- Theatre/market: elongated shared two-storey structure.
- Dormitories: mix of open-sided and closed courtyard outlines interpreted from the marked overhead image.
- Art museum and computer laboratory: added identifiable small bodies between laboratory and comprehensive groups; exact floor counts are unconfirmed and explicitly labeled as estimated heights.

All dimensions, some roof details and hidden sides are estimated from perspective screenshots. This is an exterior approximate reconstruction, not a survey model. Photo details remain accessible through the existing native card flow. No entrance/indoor navigation was added.

Verification: Node checks cover unique IDs, provided floor counts, per-building model coverage, courtyard containment, characteristic annexes and label overlap; Android debug build plus emulator inspection cover rendering and building picking.
