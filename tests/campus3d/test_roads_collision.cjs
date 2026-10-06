const assert = require('node:assert/strict');
const layout = require('../../app/src/main/assets/campus3d/campus-layout.js');
const data = require('../../app/src/main/assets/campus3d/campus-data.js');
const models = require('../../app/src/main/assets/campus3d/campus-footprints.js');

layout.calibrate(data, models);

// Point in polygon test
function pointInPoly(p, poly) {
  let inside = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const xi = poly[i][0], yi = poly[i][1];
    const xj = poly[j][0], yj = poly[j][1];
    const intersect = ((yi > p[1]) !== (yj > p[1])) &&
      (p[0] < (xj - xi) * (p[1] - yi) / (yj - yi) + xi);
    if (intersect) inside = !inside;
  }
  return inside;
}

// Distance from point to line segment
function distToSegmentSquared(p, v, w) {
  const l2 = (v[0] - w[0]) * (v[0] - w[0]) + (v[1] - w[1]) * (v[1] - w[1]);
  if (l2 === 0) return (p[0] - v[0]) * (p[0] - v[0]) + (p[1] - v[1]) * (p[1] - v[1]);
  let t = ((p[0] - v[0]) * (w[0] - v[0]) + (p[1] - v[1]) * (w[1] - v[1])) / l2;
  t = Math.max(0, Math.min(1, t));
  const projX = v[0] + t * (w[0] - v[0]);
  const projY = v[1] + t * (w[1] - v[1]);
  return (p[0] - projX) * (p[0] - projX) + (p[1] - projY) * (p[1] - projY);
}

// Build list of all building reference polygons
const buildingPolys = [];
for (const [id, p] of Object.entries(layout.placements)) {
  if (p.quad) buildingPolys.push({ id, poly: p.quad });
  else if (p.outline) buildingPolys.push({ id, poly: p.outline });
}

assert(layout.roads.length >= 25, 'Expected comprehensive road network');
assert(buildingPolys.length >= 20, 'Expected all building footprints registered');

// Test each road segment for collisions with building interiors
let collisionCount = 0;
const collidingPairs = [];

for (const road of layout.roads) {
  const halfWidthPx = (road.width * 4) / 2;
  for (let i = 0; i < road.points.length - 1; i++) {
    const p1 = road.points[i];
    const p2 = road.points[i + 1];
    const segLen = Math.hypot(p2[0] - p1[0], p2[1] - p1[1]);
    const steps = Math.max(2, Math.ceil(segLen / 2));

    for (let s = 0; s <= steps; s++) {
      const t = s / steps;
      const px = p1[0] + t * (p2[0] - p1[0]);
      const py = p1[1] + t * (p2[1] - p1[1]);
      const pt = [px, py];

      for (const b of buildingPolys) {
        // If center of road enters building interior
        if (pointInPoly(pt, b.poly)) {
          collisionCount++;
          collidingPairs.push({ roadSeg: [p1, p2], building: b.id });
        }
      }
    }
  }
}

assert.equal(collisionCount, 0, `Detected ${collisionCount} road-building collisions: ` + JSON.stringify(collidingPairs.slice(0, 5)));
console.log('Zero road-building collisions verified across entire campus network');
