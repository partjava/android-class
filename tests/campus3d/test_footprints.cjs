const assert=require('node:assert/strict');
const models=require('../../app/src/main/assets/campus3d/campus-footprints.js');
const {buildings}=require('../../app/src/main/assets/campus3d/campus-data.js');
for(const b of buildings){assert(models[b.id],b.id+' lacks individual reference model');assert(models[b.id].source);}
assert.equal(models.library.holes.length,0,'Library is a bent lakeside building, not a courtyard ring');
assert(models.library.outer.length>6);
assert(models.admin_1.annex.length>0,'Comprehensive building 1 needs its side block');
assert(models.teach_1.infill.length>0,'Teaching building 1 has a low block in its courtyard');
assert(models.gym.roof==='arena','Gym needs its own roof construction');
assert(models.theater_market.outer[2][1]-models.theater_market.outer[0][1]>40,'Theatre/market is elongated');
assert.notDeepEqual(models.teach_1.outer,models.teach_3.outer);
assert.notDeepEqual(models.dorm_1.outer,models.dorm_10.outer);
function inside(p,loop){let hit=false;for(let i=0,j=loop.length-1;i<loop.length;j=i++){const a=loop[i],b=loop[j];if((a[1]>p[1])!==(b[1]>p[1])&&p[0]<(b[0]-a[0])*(p[1]-a[1])/(b[1]-a[1])+a[0])hit=!hit;}return hit;}
for(const m of Object.values(models)){
 assert(m.outer.length>=4);
 for(const loop of [m.outer,...m.holes])for(const p of loop)assert(p.length===2&&p.every(Number.isFinite));
 for(const hole of m.holes)for(const p of hole)assert(inside(p,m.outer),'Courtyard must stay inside exterior footprint: '+m.source);
}
console.log('Individual footprint and architectural feature checks passed');
