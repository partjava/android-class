/* Ground geometry traced in the same reference coordinates as buildings. */
(function(){window.buildCampusGround=function({scene,flat,box,mat}){
 const L=CampusLayout,W=L.world;
 const polygon=(points,y,color)=>flat(points.map(W),y,color);
 const base=new THREE.Shape(L.boundary.map(p=>new THREE.Vector2(p[0]/4,-p[1]/4)));
 const geo=new THREE.ExtrudeGeometry(base,{depth:4,bevelEnabled:false});geo.rotateX(-Math.PI/2);
 const land=new THREE.Mesh(geo,mat('#9eb98b'));land.position.y=-4;land.receiveShadow=true;scene.add(land);
 polygon(L.plaza,.06,'#d8ccb6');polygon([[335,170],[520,137],[549,445],[444,415],[360,365]],.06,'#dbd5c6');
 polygon(L.grass,.08,'#a5bf8c');
 function stroke(points,width,color='#e4dccc',y=.12){for(let i=1;i<points.length;i++){const a=W(points[i-1]),b=W(points[i]),dx=b[0]-a[0],dz=b[1]-a[1];const m=box(width,.08,Math.hypot(dx,dz), (a[0]+b[0])/2,y,(a[1]+b[1])/2,color);m.rotation.y=Math.atan2(dx,dz);}}
 for(const road of L.roads)stroke(road.points,road.width);
 const shore=new THREE.CatmullRomCurve3(L.lake.map(p=>new THREE.Vector3(p[0],0,p[1])),true,'centripetal').getPoints(100).map(p=>[p.x,p.z]);
 polygon(shore,.23,'#318e91');
 const line=(points,y=.4,color='#f2ecd9')=>{const mesh=new THREE.Line(new THREE.BufferGeometry().setFromPoints(points.map(p=>new THREE.Vector3(p[0]/4,y,p[1]/4))),new THREE.LineBasicMaterial({color}));scene.add(mesh);};
 // Retain curved turns, without replacing the slanted athletic field by an axis-aligned capsule.
 const curve=new THREE.CatmullRomCurve3(L.track.map(p=>new THREE.Vector3(p[0],0,p[1])),true,'centripetal');
 const track=curve.getPoints(100).map(p=>[p.x,p.z]);polygon(track,.25,'#bb786d');
 const c=[638,266];for(const scale of [.98,.94,.9,.86])line(track.map(p=>[c[0]+(p[0]-c[0])*scale,c[1]+(p[1]-c[1])*scale]),.29,'#deb0a4');
 polygon(L.pitch,.32,'#648c66');line([...L.pitch,L.pitch[0]]);
 const pitchPoint=(u,v)=>L.bilinear(L.pitch,u,v);
 line([pitchPoint(0,.5),pitchPoint(1,.5)]);
 const circle=[];for(let i=0;i<=60;i++)circle.push(pitchPoint(.5+.14*Math.cos(i*Math.PI/30),.5+.08*Math.sin(i*Math.PI/30)));line(circle);
 for(const v of [0,1]){const end=v===0?.17:.83;line([pitchPoint(.22,v),pitchPoint(.22,end),pitchPoint(.78,end),pitchPoint(.78,v)]);}
 L.courts.forEach((quad,index)=>{const rows=index===0?3:2,cols=2;polygon(quad,.25,index===2?'#6ea3aa':'#799e8f');for(let r=0;r<rows;r++)for(let c=0;c<cols;c++){const p=(u,v)=>L.bilinear(quad,(c+u)/cols,(r+v)/rows);const rect=[p(.09,.08),p(.91,.08),p(.91,.92),p(.09,.92)];line([...rect,rect[0]]);line([p(.09,.5),p(.91,.5)]);}});
 if(window.AcademicTrace)AcademicTrace.buildParking({scene,flat,mat});
 // Trees follow traced avenues; exclude building, water and parking polygons.
 function inside(p,loop){let hit=false;for(let i=0,j=loop.length-1;i<loop.length;j=i++){const a=loop[i],b=loop[j];if((a[1]>p[1])!==(b[1]>p[1])&&p[0]<(b[0]-a[0])*(p[1]-a[1])/(b[1]-a[1])+a[0])hit=!hit;}return hit;}
 const buildings=CampusData.buildings.flatMap(b=>{const m=CampusFootprints[b.id];return [m.outer,...(m.links||[]).map(link=>link.outline)].map(loop=>loop.map(p=>[p[0]*4+b.x*4,p[1]*4+b.z*4]));});
 if(window.AcademicTrace)buildings.push(AcademicTrace.parkingWorld.map(p=>p.map(v=>v*4)));
 if(CampusData.regions)buildings.push(...CampusData.regions.filter(r=>!['plaza','lake'].includes(r.kind)).map(r=>r.outline.map(p=>p.map(v=>v*4))));
 const trunkGeo=new THREE.CylinderGeometry(.3,.4,1.7,5),leafGeo=new THREE.IcosahedronGeometry(1.8,0),trunkMat=mat('#897d61'),leafMat=mat('#628664');
 for(const road of L.roads.slice(0,8))for(let i=1;i<road.points.length;i++){const a=road.points[i-1],b=road.points[i],dx=b[0]-a[0],dz=b[1]-a[1],len=Math.hypot(dx,dz);for(let t=18;t<len;t+=42)for(const sign of [-1,1]){const offset=(road.width/2+3)*4,p=[a[0]+dx*t/len-sign*dz/len*offset,a[1]+dz*t/len+sign*dx/len*offset];if(!inside(p,L.boundary)||inside(p,L.lake)||buildings.some(loop=>inside(p,loop)))continue;const [x,z]=W(p),trunk=new THREE.Mesh(trunkGeo,trunkMat),leaf=new THREE.Mesh(leafGeo,leafMat);trunk.position.set(x,.85,z);leaf.position.set(x,3,z);leaf.castShadow=true;scene.add(trunk,leaf);}}
};})();
