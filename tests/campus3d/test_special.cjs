const assert=require('node:assert/strict');
const trace=require('../../app/src/main/assets/campus3d/special-trace.js'),data=require('../../app/src/main/assets/campus3d/campus-data.js'),models=require('../../app/src/main/assets/campus3d/campus-footprints.js'),layout=require('../../app/src/main/assets/campus3d/campus-layout.js');
layout.calibrate(data,models);require('../../app/src/main/assets/campus3d/teaching-trace.js').apply(data,models);require('../../app/src/main/assets/campus3d/dorm-trace.js').apply(data,models);require('../../app/src/main/assets/campus3d/academic-trace.js').apply(data,models);
const before=JSON.stringify(data.buildings.filter(b=>!['library','gym','canteen_main','theater_market'].includes(b.id)).map(b=>[b,models[b.id]]));
trace.apply(data,models,layout);
assert.equal(data.buildings.find(b=>b.id==='library').floors,4);assert.equal(data.buildings.find(b=>b.id==='library').basements,1);
assert.equal(data.buildings.find(b=>b.id==='gym').floors,3);assert.equal(data.buildings.find(b=>b.id==='canteen_main').floors,3);assert.equal(data.buildings.find(b=>b.id==='theater_market').floors,2);
assert(models.library.outer.length>6);assert.equal(models.library.holes.length,0);
assert.equal(models.gym.arenaCenter.length,4);assert(trace.regions.find(r=>r.id==='gym_plaza').outline.length!==models.gym.outer.length,'Gym parcel and building must have separate contours');
assert.equal(data.regions.length,8);assert.equal(new Set([...data.buildings,...data.regions].map(b=>b.id)).size,data.buildings.length+data.regions.length);
assert.equal(trace.regions.filter(r=>r.kind==='basketball').length,2);
assert(data.regions.every(r=>r.region&&r.floors===undefined));
assert.equal(layout.courts.length,0,'Generic court rectangles must be superseded');
assert.equal(layout.lake.length,trace.regions.find(r=>r.id==='lake').outline.length);
assert.equal(JSON.stringify(data.buildings.filter(b=>!['library','gym','canteen_main','theater_market'].includes(b.id)).map(b=>[b,models[b.id]])),before);
for(const [id,s]of Object.entries(trace.shapes)){const b=data.buildings.find(b=>b.id===id);for(let i=0;i<s.outer.length;i++){const q=trace.world(s.outer[i]),p=models[id].outer[i];assert(Math.abs(p[0]+b.x-q[0])<1e-9&&Math.abs(p[1]+b.z-q[1])<1e-9);}}
console.log('Special parcels, separate gym mass, lake/library contours, confirmed floors and prior models passed');
