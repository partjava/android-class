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

## 2026-10-04 region calibration

Replaced the regular road grid and guessed building centers with hand-traced coordinates from a single satellite overview: `Desktop/jiang/map/全景2.jpg`, resized to 942 × 2048, cropped at (130,730)-(790,1360), then enlarged to 1320 × 1260. Coordinates are retained as reference pixels in campus-layout.js and converted uniformly at four pixels per world unit. This is visual calibration, not a geographic scale or surveyed boundary.

Campus-layout.js places all 25 buildings, with a bent 14-point library footprint; retains the separate dormitory courtyard topology; maps courtyard, annex and roof contours into skewed building quadrilaterals. The theatre/market now extends east-west. Gym roof geometry derives from its calibrated footprint. Campus-ground.js uses the same reference for slanted roads, irregular lakeshore, central lawn, track, courts and plaza. Trees follow avenues and exclude the lake and building polygons.

Verified: four Node suites (floor counts, architecture, labels and reference calibration), Android offline debug build, and local Chromium WebGL rendering at 420×780 and 1100×800 with search, classification, top view, rotation, zoom and photo-link checks. Android emulator launch was attempted but shell commands did not return; this revision's on-device rendering is therefore unverified. Hidden facades, roof details and dimensions remain approximate; no entrance or indoor navigation added.

## Teaching area from user green-line annotation

teaching-trace.js supersedes only the four teaching buildings after campus-layout calibration. Source coordinates are retained at the annotated screenshot's 942×2048 resolution. One shared scale and translation preserves all relative shapes and spacing. Teaching 1 is a recessed body with an upper irregular courtyard and an open west-side lower court, plus the central cross wing. Teaching 2 retains its recessed northwest outline; 3 and 4 have individually traced courtyard rings. Three connecting corridor polygons are traced with estimated height (8 world units); all teaching buildings retain five floors. Roof wings follow the traced polygons, and concave roof caps use polygon triangulation. No entrance or indoor navigation added.

Regression checks cover courtyard containment, the recessed teaching-1 silhouette, roof wings, connecting pieces and uniform coordinate scaling. Browser-rendered local top and perspective previews saved outside the repository.

## Dormitories from user green-line annotation

Added dorm-trace.js after teaching-trace.js. All ten numbered dorms and the small dorm share one transform from the user's 942×2048 marked image (7.8 reference pixels per scene unit). Every numbered dorm uses an individually traced open or bent exterior polygon rather than a sealed courtyard ring. Dorms 4 and 3 retain short L-shaped returns; dorm 1 has offset upper and lower wings; dorms 7–10 preserve their different opening directions and slopes. The small dorm retains a triangular cutout. Roof polygons are divided into separate wings following the marked wall outline; heights stay at six floors for numbered dorms and three for the small dorm.

Checks: eleven-model coverage, traced-vertex coordinate equivalence, nonzero polygon area, open courtyard topology, floor counts and unchanged teaching geometry. Local Chromium top/perspective previews are outside the repo; search, filters, rotation, zoom and building picking also checked in the integrated scene. Roof ridges, facade details and physical dimensions remain estimates. Existing navigation controls, categories and photo details remain in place.

## Laboratory/comprehensive area from user annotation

academic-trace.js supersedes only lab_1, lab_2, admin_1, admin_2, art_museum and computer_center. It retains reference pixels from the user's 942×2048 annotated image under one uniform transform (10 pixels per scene unit). Lab bodies keep stepped southern outlines and a separately extruded low connecting corridor. The art museum retains its small upper cutout and stepped side projections; the computer laboratory has its own irregular attached body. Comprehensive buildings retain the angled northern connecting band, courtyard contours and northwest recess. The two laboratory and two comprehensive buildings retain five floors; art/computer floor counts remain explicitly unconfirmed estimates.

Parking is a traced six-point ground polygon with the eastern clipped corner and four schematic rows of stall markings. Trees are excluded from parking. Roof subdivisions, unmarked laboratory courtyard edges, connecting heights and facade details are estimates from the supplied photos. No entrances or indoor navigation added.

Verification: academic contour/containment/floor regression suite plus existing teaching/dorm/region/label checks; Chromium local WebGL top/perspective previews and academic search/photo-detail links. No emulator was started or controlled for this update.

## Special areas from the three additional user references

special-trace.js traces library, gym mass and surrounding plaza, canteen and the shared supermarket/theatre from the marked 1130x1067 overview. It adds eight searchable and clickable ground regions: lake, gym plaza, north/west basketball, badminton, volleyball, snack street and driving school. Gym building volume is distinct from the outlined plaza. Library keeps four above-ground floors plus basement; gym/canteen keep three; supermarket/theatre keeps two. West basketball and snack street have small westward placement adjustments to avoid overlap with the separately calibrated dormitory silhouettes. Court counts and markings, roof detail and dimensions remain schematic; no entrance or indoor navigation is added.

Verified eight geometry suites, Chromium mobile/desktop rendering and camera/building picking, academic photo links, all eight new region searches, region detail isolation and building photo restoration. Offline assembleDebug passed. No emulator was started or controlled.
