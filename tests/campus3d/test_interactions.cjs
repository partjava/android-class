const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const assets = path.resolve(__dirname, '../../app/src/main/assets/campus3d');
const THREE = require(path.join(assets, 'three.min.js'));
const elements = new Map();
const canvasEvents = {};
let renderCount = 0;
function element(id) {
  if (!elements.has(id)) elements.set(id, {
    children: [],
    style: {}, hidden: false, classList: { add() {}, remove() {}, toggle() {} },
    append(child) { this.children.push(child); }, addEventListener() {}, setPointerCapture() {}, setAttribute() {},
    getBoundingClientRect: () => ({ top: 600, bottom: 80 }), click() { this.onclick(); }
  });
  return elements.get(id);
}
const canvas = element('canvas');
canvas.addEventListener = (name, listener) => { canvasEvents[name] = listener; };
class Renderer {
  constructor() { this.domElement = canvas; this.shadowMap = {}; }
  setPixelRatio() {} setSize() {} render() { renderCount++; }
}
const places = [{ id: 'teach_1', name: '教1', category: '教学', x: 100, z: 100, floors: 5 }];
const region = { id: 'lake', name: '情缘湖', category: '文体', x: 140, z: 140, region: true };
const context = {
  THREE: { ...THREE, WebGLRenderer: Renderer }, SceneUtils: require(path.join(assets, 'scene-utils.js')),
  CampusData: { buildings: places, regions: [region] },
  SpecialTrace: { render: () => [{ b: region, group: new THREE.Group(), surface: new THREE.Mesh(new THREE.PlaneGeometry(), new THREE.MeshStandardMaterial()) }] },
  buildCampusGround() {}, buildPhotoModel() {},
  document: { getElementById: element, createElement: () => element('created' + elements.size),
    body: { prepend() {} }, querySelector: sel => element(sel), querySelectorAll: () => [], addEventListener() {} },
  innerWidth: 420, innerHeight: 780, devicePixelRatio: 1, requestAnimationFrame(fn) { context.nextFrame = fn; },
  addEventListener() {}, performance: { now: () => 0 }
};
context.window = context;
vm.createContext(context);
// Inspect camera state in the test context without adding a production debug API.
const source = fs.readFileSync(path.join(assets, 'campus-scene.js'), 'utf8')
  .replace('window.CampusScene={', 'window.__ground=groundPoint;window.__view=()=>({theta,phi,radius,x:target.x,z:target.z,cam:camera.position.toArray()});window.CampusScene={');
vm.runInContext(source, context);
const initialView = context.__view();
let prevented = false;
assert.equal(typeof canvasEvents.webglcontextlost, 'function');
canvasEvents.webglcontextlost({ preventDefault() { prevented = true; } });
context.nextFrame(100);
assert(prevented, 'Context loss must allow browser restoration');
assert.equal(renderCount, 0, 'Lost context suspends rendering');
canvasEvents.webglcontextrestored();
context.nextFrame(200);
assert.equal(renderCount, 1, 'Restored context resumes rendering');
assert.deepEqual(context.__view(), initialView, 'GPU recovery preserves default camera');
assert.equal(element('error').style.display, 'none');
function drag() {
  canvas.onpointerdown({ pointerId: 1, clientX: 150, clientY: 300 });
  canvas.onpointermove({ pointerId: 1, clientX: 190, clientY: 320 });
  canvas.onpointerup({ pointerId: 1, clientX: 190, clientY: 320, type: 'pointerup' });
}
let before = context.__view();
drag();
let after = context.__view();
assert.notEqual(after.x, before.x, 'Default drag pans map');
assert.equal(after.theta, before.theta, 'Default drag does not orbit');
context.BuildingEditor = { isEditing: () => true };
before = context.__view(); drag(); after = context.__view();
assert.notEqual(after.x, before.x, 'Building editor allows map panning');
context.BuildingEditor = { isEditing: () => false };
element('gesture').onclick();
before = context.__view(); drag(); after = context.__view();
assert.notEqual(after.theta, before.theta, 'Explicit rotation mode still orbits');
before = context.__view();
canvas.onpointerdown({ pointerId: 1, clientX: 100, clientY: 300 });
canvas.onpointerdown({ pointerId: 2, clientX: 200, clientY: 300 });
canvas.onpointermove({ pointerId: 2, clientX: 240, clientY: 300 });
assert(context.__view().radius < before.radius, 'Two-finger spread zooms in immediately');
const anchor=context.__ground(170,300);
canvas.onpointermove({ pointerId: 2, clientX: 280, clientY: 300 });
const nextAnchor=context.__ground(190,300);
assert(anchor.distanceTo(nextAnchor)<1e-6, 'Pinch retains world position under finger midpoint');
canvas.onpointerup({ pointerId: 2, type: 'pointerup' });
const continued=context.__view();
canvas.onpointermove({ pointerId: 1, clientX: 110, clientY: 310 });
assert(context.__view().x!==continued.x||context.__view().theta!==continued.theta,'Remaining finger continues dragging');
canvas.onpointerup({ pointerId: 1, type: 'pointerup' });
element('top').onclick();
const topPhi=context.__view().phi;
drag();
assert.equal(context.__view().phi,topPhi,'Orbit rotation preserves top view angle');
context.CampusScene.select('teach_1');
before = context.__view();
element('close').onclick();
assert.deepEqual(context.__view(), before, 'Closing card preserves camera');
assert.equal(element('card').hidden, true, 'Close hides card');
context.CampusScene.select('teach_1');
assert.equal(element('card').hidden, false, 'Selecting reopens card');
// Exercise all category handlers, including region groups with no building record.
for (const el of element('filters').children) if(el!==element('label-toggle')) assert.doesNotThrow(() => el.onclick());
const labelToggle = element('label-toggle');
labelToggle.onclick();
assert.equal(element('labels').hidden, true, 'Name toggle hides map labels');
context.CampusScene.select('teach_1');
assert.equal(element('labels').hidden, true, 'Selecting does not restore hidden labels');
labelToggle.onclick();
assert.equal(element('labels').hidden, false, 'Name toggle restores map labels');
function assertCameraStable(before, after) {
  for (let i=0;i<3;i++) assert(Math.abs(before.cam[i]-after.cam[i])<1e-8, 'Stopping animation preserves camera');
}
element('cruise').onclick();
context.nextFrame(1000);
before=context.__view();
context.CampusScene.stopMotion();
canvas.onpointerdown({ pointerId: 1, clientX: 100, clientY: 300 });
canvas.onpointermove({ pointerId: 1, clientX: 100, clientY: 300 });
assertCameraStable(before,context.__view());
canvas.onpointerup({ pointerId: 1, type:'pointercancel' });
context.CampusScene.select('teach_1');
element('fly').onclick();
context.nextFrame(1600);
before=context.__view();
canvas.onwheel({ deltaY:0, preventDefault() {} });
assertCameraStable(before,context.__view());
element('fly').onclick();
context.nextFrame(750);
before=context.__view();
context.CampusScene.stopMotion();
canvas.onwheel({ deltaY:0, preventDefault() {} });
assertCameraStable(before,context.__view());

// Ground should not draw a second stationary lake beneath the movable region.
const groundSource = fs.readFileSync(path.join(assets, 'campus-ground.js'), 'utf8');
assert(!groundSource.includes('const lakeMesh='), 'No stationary duplicate lake');
const trace = require(path.join(assets, 'special-trace.js'));
global.THREE = THREE;
const scene = new THREE.Scene();
const records = trace.render({ regions: [{ ...region, kind: 'lake', outline: [[0,0],[10,0],[10,10],[0,10]], color: '#318e91' }] }, {
  scene, mat: color => new THREE.MeshStandardMaterial({ color }),
  flat(points, y, color, parent) { const m = new THREE.Mesh(new THREE.PlaneGeometry(), new THREE.MeshStandardMaterial({ color })); parent.add(m); return m; }
});
assert(records[0].group.children.some(child => child.isLine), 'Lake shore belongs to movable lake group');
const line = records[0].group.children.find(child => child.isLine);
records[0].group.position.set(3, 0, 4);
records[0].group.rotation.y = Math.PI / 6;
scene.updateMatrixWorld(true);
assert.deepEqual(line.matrixWorld.elements, records[0].surface.matrixWorld.elements, 'Shore follows lake translation and rotation');
console.log('Map drag, editor drag, rotation, card close, filters and lake grouping passed');
