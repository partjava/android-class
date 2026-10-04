const assert=require('node:assert/strict');
const layout=require('../../app/src/main/assets/campus3d/campus-layout.js');
const data=require('../../app/src/main/assets/campus3d/campus-data.js'),models=require('../../app/src/main/assets/campus3d/campus-footprints.js');
layout.calibrate(data,models);
for(const b of data.buildings){const placement=layout.placements[b.id];assert.deepEqual([b.x,b.z],placement.center.map(v=>v/4));}
function inside(p,loop){let hit=false;for(let i=0,j=loop.length-1;i<loop.length;j=i++){const a=loop[i],b=loop[j];if((a[1]>p[1])!==(b[1]>p[1])&&p[0]<(b[0]-a[0])*(p[1]-a[1])/(b[1]-a[1])+a[0])hit=!hit;}return hit;}
for(const [id,m]of Object.entries(models)){for(const hole of m.holes)for(const p of hole)assert(inside(p,m.outer),'Calibrated courtyard outside '+id);}
assert(models.theater_market.outer[1][0]-models.theater_market.outer[0][0]>30);
assert(models.dorm_10.outer[1][1]!==models.dorm_10.outer[0][1],'Northern dorm must keep slanted alignment');
assert.equal(layout.reference.width,1320);assert.equal(layout.reference.height,1260);
assert(layout.lake.length>=12,'Lake must preserve its irregular traced shore');
assert(layout.boundary[0][1]!==layout.boundary[1][1],'Northern boundary must not be rectangular');
assert(layout.grass.every(p=>p.length===2));
assert.equal(Object.keys(layout.placements).filter(id=>id.startsWith('dorm_')).length,10);
const centers=Object.values(layout.placements).map(p=>p.center);
assert(centers.every(p=>p.every(Number.isFinite)));
assert(layout.placements.theater_market.quad[1][0]-layout.placements.theater_market.quad[0][0]>100,'Theatre extends east-west in reference');
assert(layout.placements.library.outline.length>10);
const triangle=(a,b,c)=>Math.abs((b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]))/2;
for(const [id,p]of Object.entries(layout.placements))if(p.quad){assert(triangle(p.quad[0],p.quad[1],p.quad[2])>10,id+' degenerate footprint');}
console.log('Reference-coordinate region and placement checks passed');
