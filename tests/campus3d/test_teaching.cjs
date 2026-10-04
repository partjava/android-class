const assert=require('node:assert/strict');
const trace=require('../../app/src/main/assets/campus3d/teaching-trace.js');
const data=require('../../app/src/main/assets/campus3d/campus-data.js'),models=require('../../app/src/main/assets/campus3d/campus-footprints.js'),layout=require('../../app/src/main/assets/campus3d/campus-layout.js');
layout.calibrate(data,models);trace.apply(data,models);
assert(models.teach_1.outer.length>4,'Teaching 1 must retain the west opening, not a rectangular ring');
assert.equal(models.teach_1.holes[0].length,6,'Upper courtyard has the marked recess beside the cross wing');
assert.equal(models.teach_1.roofWings.length,6);
assert.equal(models.teach_1.links.length,2,'Teaching 1 needs the link to teaching 3 and south link');
assert.equal(models.teach_4.links.length,1,'Teaching 4 links to teaching 2');
for(let i=1;i<=4;i++){assert.equal(data.buildings.find(b=>b.id==='teach_'+i).floors,5);assert.equal(models['teach_'+i].roof,'traced');}
function inside(p,loop){let hit=false;for(let i=0,j=loop.length-1;i<loop.length;j=i++){const a=loop[i],b=loop[j];if((a[1]>p[1])!==(b[1]>p[1])&&p[0]<(b[0]-a[0])*(p[1]-a[1])/(b[1]-a[1])+a[0])hit=!hit;}return hit;}
for(let i=1;i<=4;i++){const m=models['teach_'+i];for(const hole of m.holes)for(const p of hole)assert(inside(p,m.outer),'Marked courtyard escapes teaching '+i);}
const a=trace.world([48,756]),b=trace.world([144,852]);assert.deepEqual(b.map((v,i)=>v-a[i]),[10,10],'One uniform scale must preserve teaching-area proportions');
console.log('Marked teaching contours, courtyards, connections and floors passed');
