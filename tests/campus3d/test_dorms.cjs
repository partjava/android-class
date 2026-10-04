const assert=require('node:assert/strict');
const trace=require('../../app/src/main/assets/campus3d/dorm-trace.js');
const data=require('../../app/src/main/assets/campus3d/campus-data.js'),models=require('../../app/src/main/assets/campus3d/campus-footprints.js');
require('../../app/src/main/assets/campus3d/campus-layout.js').calibrate(data,models);require('../../app/src/main/assets/campus3d/teaching-trace.js').apply(data,models);
const teachingBefore=JSON.stringify(data.buildings.filter(b=>b.id.startsWith('teach_')).map(b=>[b,models[b.id]]));
trace.apply(data,models);
assert.equal(Object.keys(trace.shapes).length,11);
assert.equal(models.dorm_4.outer.length,6,'Dorm 4 is an L, not a courtyard ring');
assert.equal(models.dorm_3.outer.length,6,'Dorm 3 retains its shorter return');
assert.equal(models.dorm_1.outer.length,8,'Dorm 1 retains offset upper and lower wings');
assert.equal(models.small_dorm.holes.length,1);
assert.equal(models.small_dorm.holes[0].length,3,'Small dorm has the triangular cutout shown by green lines');
for(let n=1;n<=10;n++){const id='dorm_'+n;assert.equal(models[id].holes.length,0,id+' opening must not become a sealed courtyard');assert.equal(data.buildings.find(b=>b.id===id).floors,6);assert.equal(models[id].roof,'traced');}
assert.equal(data.buildings.find(b=>b.id==='small_dorm').floors,3);
for(const [id,shape]of Object.entries(trace.shapes)){const b=data.buildings.find(b=>b.id===id),m=models[id];for(let i=0;i<shape.outer.length;i++){const expected=trace.world(shape.outer[i]);assert(Math.abs(m.outer[i][0]+b.x-expected[0])<1e-9);assert(Math.abs(m.outer[i][1]+b.z-expected[1])<1e-9);}for(const loop of [m.outer,...m.holes,...m.roofWings])assert(loop.every(p=>p.every(Number.isFinite)));}
assert.equal(JSON.stringify(data.buildings.filter(b=>b.id.startsWith('teach_')).map(b=>[b,models[b.id]])),teachingBefore,'Dorm calibration must leave marked teaching buildings intact');
function area(loop){let sum=0;for(let i=0;i<loop.length;i++){const a=loop[i],b=loop[(i+1)%loop.length];sum+=a[0]*b[1]-b[0]*a[1];}return Math.abs(sum)/2;}
for(const [id,m]of Object.entries(models).filter(([id])=>id.startsWith('dorm_')||id==='small_dorm'))assert(area(m.outer)>15,id+' silhouette has no usable area');
console.log('Eleven marked dorm silhouettes, openings, shared scale, floors and teaching preservation passed');
