const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const assets = path.resolve(__dirname, '../../app/src/main/assets/campus3d');
const html = fs.readFileSync(path.join(assets, 'index.html'), 'utf8');
const css = fs.readFileSync(path.join(assets, 'campus-ui.css'), 'utf8');
const sceneJs = fs.readFileSync(path.join(assets, 'campus-scene.js'), 'utf8');
const roadJs = fs.readFileSync(path.join(assets, 'road-editor.js'), 'utf8');
const bldJs = fs.readFileSync(path.join(assets, 'building-editor.js'), 'utf8');

// 1. Verify fixed plus/minus buttons
const fixedMatch = html.match(/<div class="tools-fixed"[^>]*>([\s\S]*?)<\/div>/);
assert(fixedMatch, '.tools-fixed container exists');
const fixedIds = [...fixedMatch[1].matchAll(/id="([^"]+)"/g)].map(m => m[1]);
assert.deepEqual(fixedIds, ['plus', 'minus'], 'Plus and minus buttons are pinned in fixed section');

// 2. Verify scrollable container and first 3 items (total 2 fixed + 3 scroll = 5 visible)
const scrollMatch = html.match(/<div class="tools-scroll" id="tools-scroll"[^>]*>([\s\S]*?)<\/div>/);
assert(scrollMatch, '.tools-scroll container exists with id="tools-scroll"');
const scrollIds = [...scrollMatch[1].matchAll(/id="([^"]+)"/g)].map(m => m[1]);
assert.deepEqual(
  scrollIds.slice(0, 3),
  ['mode', 'top', 'route'],
  'First 3 scrollable items are mode (沙盘), top (俯视), route (路线)'
);
assert.deepEqual(
  scrollIds.slice(3),
  ['cruise', 'gesture', 'reset'],
  'Followed by cruise, gesture, reset in scrollable container'
);

// 3. Verify road-editor and building-editor target tools-scroll and restore scroll
assert(roadJs.includes("document.getElementById('tools-scroll')"), 'road-editor targets tools-scroll');
assert(roadJs.includes('restoreToolsScroll'), 'road-editor triggers scroll restore');
assert(bldJs.includes("document.getElementById('tools-scroll')"), 'building-editor targets tools-scroll');
assert(bldJs.includes('restoreToolsScroll'), 'building-editor triggers scroll restore');

// 4. Verify CSS frosted glass, fixed area, and scrollable container
assert(css.includes('backdrop-filter:blur'), 'Frosted glass backdrop-filter applied');
assert(css.includes('.tools-fixed'), 'tools-fixed class styled in CSS');
assert(css.includes('.tools-scroll'), 'tools-scroll class styled in CSS');
assert(css.includes('overflow-y:auto'), 'Tools scroll container is vertically scrollable');
assert(css.includes('touch-action:pan-y'), 'Touch action pan-y configured for vertical scrolling');

// 5. Verify scroll position caching logic on tools-scroll in campus-scene.js
assert(sceneJs.includes('campus3d_tools_scroll'), 'localStorage key used for scroll position memory');
assert(sceneJs.includes('restoreToolsScroll'), 'restoreToolsScroll function registered');
assert(sceneJs.includes('stopPropagation'), 'Touch gestures isolated from map canvas');

console.log('✔ Fixed zoom buttons with 3-item frosted glass scrollable dock passed');
