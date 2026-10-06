const assert = require('node:assert/strict');

// Mock browser DOM and Three.js environment
globalThis.window = globalThis;
globalThis.localStorage = {
  _data: {},
  getItem(k) { return this._data[k] || null; },
  setItem(k, v) { this._data[k] = String(v); },
  removeItem(k) { delete this._data[k]; }
};

globalThis.THREE = {
  RingGeometry: class {
    constructor() {}
    rotateX() {}
  },
  MeshBasicMaterial: class {
    constructor(opt) { Object.assign(this, opt); }
  },
  Mesh: class {
    constructor(geo, mat) {
      this.geometry = geo;
      this.material = mat;
      this.position = { x: 0, y: 0, z: 0 };
      this.rotation = { x: 0, y: 0, z: 0 };
      this.visible = true;
    }
  },
  DoubleSide: 2
};

const elements = {};
function mockElement(tag, id) {
  const el = {
    tagName: tag,
    id: id || '',
    style: {},
    classList: {
      classes: new Set(),
      toggle(c, v) { if (v === undefined) v = !this.classes.has(c); if (v) this.classes.add(c); else this.classes.delete(c); return v; },
      add(c) { this.classes.add(c); },
      remove(c) { this.classes.delete(c); }
    },
    children: [],
    appendChild(c) { this.children.push(c); return c; },
    insertBefore(c) { this.children.unshift(c); return c; },
    addEventListener(ev, fn) { this['on' + ev] = fn; },
    removeEventListener() {},
    getBoundingClientRect() { return { left: 0, top: 0, width: 800, height: 600 }; }
  };
  if (id) elements[id] = el;
  return el;
}

globalThis.document = {
  body: mockElement('body'),
  querySelector(sel) {
    if (sel === '.tools') return elements['tools'] || (elements['tools'] = mockElement('div', 'tools'));
    return null;
  },
  querySelectorAll() { return []; },
  getElementById(id) {
    return elements[id] || (elements[id] = mockElement('div', id));
  },
  createElement(tag) { return mockElement(tag); }
};

const createBuildingEditor = require('../../app/src/main/assets/campus3d/building-editor.js');
assert.equal(typeof createBuildingEditor, 'function', 'Factory function exported');

createBuildingEditor();
assert(globalThis.BuildingEditor, 'BuildingEditor registered');
assert.equal(typeof globalThis.BuildingEditor.init, 'function');
assert.equal(typeof globalThis.BuildingEditor.toggleEditor, 'function');
assert.equal(typeof globalThis.BuildingEditor.selectBuilding, 'function');
assert.equal(typeof globalThis.BuildingEditor.nudge, 'function');

const mockScene = { add() {} };
const mockCamera = {};
const mockRenderer = { domElement: mockElement('canvas') };

const mockGroup = {
  position: { x: 100, y: 0, z: 200 },
  rotation: { y: 0 },
  traverse() {}
};
const groups = new Map();
groups.set('teach_1', mockGroup);

const mockLabel = {
  b: { id: 'teach_1' },
  s: { position: { x: 100, y: 15, z: 200 } }
};
const labels = [mockLabel];

const CampusData = {
  buildings: [
    { id: 'teach_1', name: '教1 (教学楼1)', x: 100, z: 200 }
  ]
};

globalThis.BuildingEditor.init({
  scene: mockScene,
  camera: mockCamera,
  renderer: mockRenderer,
  groups,
  labels,
  CampusData,
  select: () => {}
});

assert.equal(globalThis.BuildingEditor.isEditing(), false, 'Initially not editing');

globalThis.BuildingEditor.toggleEditor();
assert.equal(globalThis.BuildingEditor.isEditing(), true, 'Editing active');

globalThis.BuildingEditor.selectBuilding('teach_1');
globalThis.BuildingEditor.nudge(2.0, -1.5, 10);

assert.equal(mockGroup.position.x, 102.0, 'X nudged by +2.0m');
assert.equal(mockGroup.position.z, 198.5, 'Z nudged by -1.5m');
assert.equal(mockLabel.s.position.x, 102.0, 'Label synced X');
assert.equal(mockLabel.s.position.z, 198.5, 'Label synced Z');

const transforms = globalThis.BuildingEditor.getTransforms();
assert.equal(transforms['teach_1'].dx, 2.0);
assert.equal(transforms['teach_1'].dz, -1.5);
assert.equal(transforms['teach_1'].rot, 10);
globalThis.BuildingEditor.toggleEditor();

const mockSchoolGroup = {
  position: { x: 0, y: 0, z: 0 },
  rotation: { y: 0 },
  traverse() {}
};
groups.set('driving_school', mockSchoolGroup);

const allPlaces = [
  { id: 'teach_1', name: '教1 (教学楼1)', x: 100, z: 200, category: '教学' },
  { id: 'driving_school', name: '驾校', x: 200, z: 250, category: '生活', region: true }
];

globalThis.BuildingEditor.init({
  scene: mockScene,
  camera: mockCamera,
  renderer: mockRenderer,
  groups,
  labels,
  CampusData,
  select: () => {},
  allPlaces
});

globalThis.BuildingEditor.toggleEditor();
globalThis.BuildingEditor.selectBuilding('driving_school');
globalThis.BuildingEditor.nudge(3.0, -2.0, 0);

assert.equal(mockSchoolGroup.position.x, 3.0, 'Driving school nudged X');
assert.equal(mockSchoolGroup.position.z, -2.0, 'Driving school nudged Z');

globalThis.BuildingEditor.toggleEditor();
assert.equal(globalThis.BuildingEditor.isEditing(), false, 'Editing closed');

console.log('✔ building-editor.js unit tests passed: select, nudge buildings & region venues, labels sync verified');
