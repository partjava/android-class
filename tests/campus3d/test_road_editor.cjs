const assert = require('node:assert/strict');

// Mock browser DOM and Three.js environment for road-editor unit testing
globalThis.window = globalThis;
globalThis.localStorage = {
  _data: {},
  getItem(k) { return this._data[k] || null; },
  setItem(k, v) { this._data[k] = String(v); },
  removeItem(k) { delete this._data[k]; }
};

// Mock minimal Three.js structures used by editor
globalThis.THREE = {
  Raycaster: class {
    constructor() { this.ray = { intersectPlane: () => ({ x: 40, z: 50 }) }; }
    setFromCamera() {}
  },
  Vector2: class { constructor(x, y) { this.x = x; this.y = y; } },
  Vector3: class { constructor(x, y, z) { this.x = x; this.y = y; this.z = z; } },
  Plane: class { constructor() {} },
  Group: class {
    constructor() { this.children = []; this.visible = true; }
    add(obj) { this.children.push(obj); }
    remove(obj) {
      const idx = this.children.indexOf(obj);
      if (idx !== -1) this.children.splice(idx, 1);
    }
  },
  BufferGeometry: class {
    constructor() { this.attributes = {}; }
    setAttribute(n, a) { this.attributes[n] = a; }
    setFromPoints() { return this; }
    dispose() {}
  },
  Float32BufferAttribute: class { constructor(arr, itemSize) { this.array = arr; this.itemSize = itemSize; } },
  LineBasicMaterial: class { constructor(opt) { Object.assign(this, opt); } },
  LineSegments: class { constructor(geo, mat) { this.geometry = geo; this.material = mat; } },
  Line: class { constructor(geo, mat) { this.geometry = geo; this.material = mat; } },
  RingGeometry: class { constructor() {} rotateX() {} },
  MeshBasicMaterial: class { constructor(opt) { Object.assign(this, opt); } },
  BoxGeometry: class { constructor(w, h, d) { this.w = w; this.h = h; this.d = d; } dispose() {} },
  Mesh: class {
    constructor(geo, mat) {
      this.geometry = geo;
      this.material = mat;
      this.position = { x: 0, y: 0, z: 0, set(x, y, z) { this.x = x; this.y = y; this.z = z; } };
      this.rotation = { x: 0, y: 0, z: 0 };
      this.visible = true;
    }
  },
  DoubleSide: 2
};

// Mock minimal DOM
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
  getElementById(id) {
    return elements[id] || (elements[id] = mockElement('div', id));
  },
  createElement(tag) { return mockElement(tag); }
};

const createRoadEditor = require('../../app/src/main/assets/campus3d/road-editor.js');
const layout = require('../../app/src/main/assets/campus3d/campus-layout.js');
globalThis.CampusLayout = layout;

assert.equal(typeof createRoadEditor, 'function', 'road-editor exports factory function');

createRoadEditor();
assert(globalThis.RoadEditor, 'RoadEditor namespace registered on window');
assert.equal(typeof globalThis.RoadEditor.init, 'function', 'RoadEditor.init is callable');
assert.equal(typeof globalThis.RoadEditor.toggleEditor, 'function', 'RoadEditor.toggleEditor is callable');
assert.equal(typeof globalThis.RoadEditor.getMode, 'function', 'RoadEditor.getMode is callable');
assert.equal(typeof globalThis.RoadEditor.clearRoads, 'function', 'RoadEditor.clearRoads is callable');
assert.equal(typeof globalThis.RoadEditor.getRoads, 'function', 'RoadEditor.getRoads is callable');

const mockScene = new globalThis.THREE.Group();
const mockCamera = {};
const mockRenderer = {
  domElement: mockElement('canvas')
};

globalThis.RoadEditor.init({
  scene: mockScene,
  camera: mockCamera,
  renderer: mockRenderer
});

assert.equal(globalThis.RoadEditor.isEditing(), false, 'Initially not editing');
assert.equal(globalThis.RoadEditor.getMode(), 'point', 'Default mode is point');

globalThis.RoadEditor.toggleEditor();
assert.equal(globalThis.RoadEditor.isEditing(), true, 'Editing mode active after toggle');

globalThis.RoadEditor.clearRoads();
assert.equal(globalThis.RoadEditor.getRoads().length, 0, 'Roads cleared to 0');

globalThis.RoadEditor.toggleEditor();
assert.equal(globalThis.RoadEditor.isEditing(), false, 'Editing mode closed after second toggle');
let buildingEditing = true;
globalThis.BuildingEditor = { isEditing: () => buildingEditing, toggleEditor() { buildingEditing = false; } };
let groundMode = 'schematic';
globalThis.getGroundMode = () => groundMode;
globalThis.setGroundMode = mode => { groundMode = mode; };
const nativeData = {};
globalThis.CampusNativeBridge = { saveConfig(key, value) { nativeData[key] = value; } };
RoadEditor.toggleEditor();
assert.equal(buildingEditing, false, 'Entering road editor exits building editor');
assert.equal(groundMode, 'aerial');
RoadEditor.autoSave();
assert.deepEqual(JSON.parse(nativeData.custom_campus_roads), RoadEditor.getRoads(), 'Public autoSave persists roads');
RoadEditor.toggleEditor();
assert.equal(groundMode, 'schematic', 'Closing road editor restores original ground mode');
RoadEditor.toggleEditor();
const beforeGesture=JSON.stringify(RoadEditor.getRoads());
const dom=mockRenderer.domElement;
dom.onpointerdown({pointerId:1,clientX:100,clientY:100,pointerType:'touch',isPrimary:true});
dom.onpointerdown({pointerId:2,clientX:160,clientY:100,pointerType:'touch',isPrimary:false});
assert(RoadEditor.isGesturePaused(),'Second finger pauses drawing');
assert.equal(JSON.stringify(RoadEditor.getRoads()),beforeGesture,'Two finger gesture restores roads');
dom.onpointermove({pointerId:1,clientX:110,clientY:110});
dom.onpointerup({pointerId:2});
assert(RoadEditor.isGesturePaused(),'Remaining finger cannot draw until gesture ends');
dom.onpointerup({pointerId:1});
assert(!RoadEditor.isGesturePaused(),'A new single finger gesture may draw');
RoadEditor.toggleEditor();

console.log('✔ road-editor.js unit tests passed: state toggle, UI construction, clearRoads and mode verified');
