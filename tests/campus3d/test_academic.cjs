const assert=require('node:assert/strict');
const trace=require('../../app/src/main/assets/campus3d/academic-trace.js'),data=require('../../app/src/main/assets/campus3d/campus-data.js'),models=require('../../app/src/main/assets/campus3d/campus-footprints.js');
require('../../app/src/main/assets/campus3d/campus-layout.js').calibrate(data,models);require('../../app/src/main/assets/campus3d/teaching-trace.js').apply(data,models);require('../../app/src/main/assets/campus3d/dorm-trace.js').apply(data,models);
const before=JSON.stringify(data.buildings.filter(b=>b.id.startsWith('teach_')||b.id.startsWith('dorm_')).map(b=>[b,models[b.id]]));
trace.apply(data,models);
assert.equal(Object.keys(trace.shapes).length,6);
for(const id of ['lab_1','lab_2','admin_1','admin_2'])assert.equal(data.buildings.find(b=>b.id===id).floors,5);
assert(models.lab_1.outer.length>4);assert(models.lab_2.outer.length>4);
assert(models.art_museum.holes.length===1);assert(models.art_museum.outer.length>10);
assert(models.computer_center.outer.length>8);assert(models.admin_1.outer.length>4);
assert.equal(models.lab_1.links.length,1);
assert.equal(trace.parking.length,6,'Parking lot must preserve its clipped corner');
function inside(p,loop){let hit=false;for(let i=0,j=loop.length-1;i<loop.length;j=i++){const a=loop[i],b=loop[j];if((a[1]>p[1])!==(b[1]>p[1])&&p[0]<(b[0]-a[0])*(p[1]-a[1])/(b[1]-a[1])+a[0])hit=!hit;}return hit;}
for(const [id,s]of Object.entries(trace.shapes)){const b=data.buildings.find(b=>b.id===id),m=models[id];for(let i=0;i<s.outer.length;i++){const q=trace.world(s.outer[i]);assert(Math.abs(m.outer[i][0]+b.x-q[0])<1e-9);assert(Math.abs(m.outer[i][1]+b.z-q[1])<1e-9);}for(const hole of m.holes)for(const p of hole)assert(inside(p,m.outer),id+' courtyard outside footprint');}
assert.equal(JSON.stringify(data.buildings.filter(b=>b.id.startsWith('teach_')||b.id.startsWith('dorm_')).map(b=>[b,models[b.id]])),before);
console.log('Six academic traces, courtyard containment, parking outline, floors and prior models passed');
