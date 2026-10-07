const assert=require('node:assert/strict');
const R=require('../../app/src/main/assets/campus3d/campus-routing.js');
const roads=[{width:3,points:[[0,0],[40,0]]},{width:3,points:[[20,-20],[20,20]]}];
let result=R.findRoute(roads,{id:'a',point:[0,0]},{id:'b',point:[20,20]});
assert.equal(result.status,'ok');
assert(Math.abs(result.distance-40)<1e-6,'Crossing roads are split and joined');
result=R.findRoute([{width:3,points:[[0,0],[10,0]]},{width:3,points:[[11,0],[20,0]]}],{id:'a',point:[0,0]},{id:'b',point:[20,0]},{maxAccess:1});
assert.equal(result.status,'ok','Short visible road gaps connect');
result=R.findRoute([{width:3,points:[[0,0],[10,0]]},{width:3,points:[[30,0],[40,0]]}],{id:'a',point:[0,0]},{id:'b',point:[40,0]},{maxAccess:2});
assert.equal(result.status,'disconnected','Separate road components must not be joined by invented routes');
assert.equal(R.findRoute([],{id:'a',point:[0,0]},{id:'b',point:[2,2]}).status,'no-roads');
assert.equal(R.findRoute(roads,{id:'a',point:[0,0]},{id:'a',point:[0,0]}).status,'same-place');
const obstacle={id:'lake',polygon:[[9,-3],[11,-3],[11,3],[9,3]]};
result=R.findRoute([{width:3,points:[[0,0],[20,0]]}],{id:'a',point:[0,0]},{id:'b',point:[20,0]},{obstacles:[obstacle],maxAccess:1});
assert.notEqual(result.status,'ok','Road across lake is blocked');
const blockedAccess={id:'building',polygon:[[4,1],[6,1],[6,9],[4,9]]};
assert(R.blocked([0,5],[10,5],[blockedAccess]),'Connector must not cross another building');
const base=JSON.stringify(roads);
R.findRoute(roads,{id:'a',point:[0,0]},{id:'b',point:[20,20]});
assert.equal(JSON.stringify(roads),base,'Routing does not mutate saved roads');
// Exercise the user's current 210-road network and calibrated building positions.
const data=require('../../app/src/main/assets/campus3d/campus-data.js');
const models=require('../../app/src/main/assets/campus3d/campus-footprints.js');
const layout=require('../../app/src/main/assets/campus3d/campus-layout.js');
layout.calibrate(data,models);
for(const file of ['teaching-trace','dorm-trace','academic-trace'])require('../../app/src/main/assets/campus3d/'+file+'.js').apply(data,models);
require('../../app/src/main/assets/campus3d/special-trace.js').apply(data,models,layout);
const campusObstacles=[];
for(const b of data.buildings){
 const t=data.defaultTransforms[b.id]||{dx:0,dz:0,rot:0},angle=t.rot*Math.PI/180;
 const transform=p=>[b.x+t.dx+p[0]*Math.cos(angle)+p[1]*Math.sin(angle),b.z+t.dz-p[0]*Math.sin(angle)+p[1]*Math.cos(angle)];
 for(const polygon of [models[b.id].outer,...(models[b.id].links||[]).map(l=>l.outline)])campusObstacles.push({id:b.id,polygon:polygon.map(transform)});
}
for(const r of data.regions)if(r.kind==='lake'){
 const t=data.defaultTransforms[r.id],angle=t.rot*Math.PI/180;
 campusObstacles.push({id:r.id,polygon:r.outline.map(p=>[r.x+t.dx+(p[0]-r.x)*Math.cos(angle)+(p[1]-r.z)*Math.sin(angle),r.z+t.dz-(p[0]-r.x)*Math.sin(angle)+(p[1]-r.z)*Math.cos(angle)])});
}
const liveRoads=layout.roads.map(r=>({...r,points:r.points.map(layout.world)}));
const endpoint=id=>{const b=data.buildings.find(b=>b.id===id),t=data.defaultTransforms[id]||{dx:0,dz:0};return {id,point:[b.x+t.dx,b.z+t.dz]};};
for(const [a,b] of [['teach_1','library'],['dorm_1','canteen_main'],['gym','teach_2']]){
 const route=R.findRoute(liveRoads,endpoint(a),endpoint(b),{obstacles:campusObstacles});
 assert.equal(route.status,'ok',a+' to '+b+' uses the current road network');
 for(let i=1;i<route.roadPoints.length;i++)assert(!R.blocked(route.roadPoints[i-1],route.roadPoints[i],campusObstacles),'Road path avoids the current building and lake polygons');
}
console.log('Road intersections, gaps, disconnected routes, obstacles and immutable road data passed');
