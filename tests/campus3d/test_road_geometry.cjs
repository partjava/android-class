const assert=require('node:assert/strict');
const G=require('../../app/src/main/assets/campus3d/road-geometry.js');
const roads=[{width:6,points:[[0,0],[80,0]]},{width:6,points:[[40,-40],[40,40]]},{width:3,points:[[80,0],[100,20],[120,20]]}];
const snapshot=JSON.stringify(roads),p=G.plan(roads);
assert.equal(JSON.stringify(roads),snapshot);
assert(p.curbs.length&&p.asphalt.length&&p.paving.length);
assert(p.curbs.some(poly=>poly.length>=20),'Round joins replace square caps');
assert(p.markings.length>0);
for(const poly of p.markings){const cx=poly.reduce((s,v)=>s+v[0],0)/poly.length,cz=poly.reduce((s,v)=>s+v[1],0)/poly.length;assert(!(Math.abs(cx-10)<3.6&&Math.abs(cz)<3.6),'Centerlines leave junction clear');}
assert.deepEqual(G.plan([{width:6,points:[[0,0],[0,0]]}]).markings,[]);
const THREE=require('../../app/src/main/assets/campus3d/three.min.js');
const group=G.build(THREE,roads);
assert(group.children.length<=5,'Batch road layers into a small number of meshes');
assert(group.children.every(m=>Array.from(m.geometry.attributes.position.array).every(Number.isFinite)));
console.log('Round road joins, junction-clear markings, immutable coordinates and batched meshes passed');
