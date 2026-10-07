const assert=require('node:assert/strict'),T=require('../../app/src/main/assets/campus3d/road-tools.js');
const roads=[{width:3,points:[[0,0],[80,0]]},{width:3,points:[[40,-40],[40,40]]},{width:3,points:[[200,200],[240,200]]}];const original=JSON.stringify(roads);
assert.deepEqual(T.snapPoint([3,2],roads).point,[0,0]);assert.equal(T.snapPoint([22,2],roads).kind,'segment');assert.deepEqual(T.snapPoint([100,100],roads).point,[100,100]);
const check=T.analyze(roads);assert.equal(check.components,2);assert.deepEqual(check.isolated,[2]);assert.equal(check.deadEnds.length,6);assert.equal(JSON.stringify(roads),original);
assert.equal(T.analyze([{width:3,points:[[0,0],[40,0]]},{width:3,points:[[50,0],[90,0]]}]).gaps.length,2);
assert.equal(T.analyze([{width:3,points:[[0,0],[40,0],[40,40],[0,0]]}]).deadEnds.length,0);
assert.equal(T.analyze([]).components,0);console.log('Road endpoint/segment snapping, crossing connectivity, gaps and isolated roads passed');
