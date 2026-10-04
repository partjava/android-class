/* Exterior massing interpreted per reference photograph. Geometry remains approximate. */
(function(){
function shapeOf(outer,holes=[]){const shape=new THREE.Shape(outer.map(p=>new THREE.Vector2(p[0],-p[1])));for(const hole of holes)shape.holes.push(new THREE.Path(hole.map(p=>new THREE.Vector2(p[0],-p[1]))));return shape;}
window.buildPhotoModel=function(b,g,api){
 const {mat,box,pickables}=api,m=CampusFootprints[b.id],h=b.floors*3.2;
 g.userData.reference=m.source;
 function register(mesh){mesh.userData.id=b.id;mesh.castShadow=true;mesh.receiveShadow=true;g.add(mesh);pickables.push(mesh);return mesh;}
 function volume(outer,holes,height,y,color){const geo=new THREE.ExtrudeGeometry(shapeOf(outer,holes),{depth:height,bevelEnabled:false});geo.rotateX(-Math.PI/2);const mesh=new THREE.Mesh(geo,mat(color));mesh.position.y=y;return register(mesh);}
 function surface(vertices,faces,color){const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(vertices.flat(),3));geo.setIndex(faces);geo.computeVertexNormals();const material=mat(color);material.side=THREE.DoubleSide;return register(new THREE.Mesh(geo,material));}
 function ringRoof(outer,hole,y,rise){const v=[];for(let i=0;i<4;i++){const a=outer[i],c=hole[i];v.push([a[0],y,a[1]],[(a[0]+c[0])/2,y+rise,(a[1]+c[1])/2],[c[0],y,c[1]]);}const indices=[];for(let i=0;i<4;i++){const a=i*3,n=((i+1)%4)*3;indices.push(a,n,n+1,a,n+1,a+1,a+1,n+1,n+2,a+1,n+2,a+2);}surface(v,indices,'#62656b');}
 function hipRoof(outer,y,rise,color='#686970'){const cx=outer.reduce((s,p)=>s+p[0],0)/outer.length,cz=outer.reduce((s,p)=>s+p[1],0)/outer.length;const v=[];for(const p of outer)v.push([p[0],y,p[1]]);for(const p of outer)v.push([cx+(p[0]-cx)*.40,y+rise,cz+(p[1]-cz)*.65]);const indices=[],n=outer.length;for(let i=0;i<n;i++){const j=(i+1)%n;indices.push(i,j,j+n,i,j+n,i+n);}const cap=v.slice(n).map(p=>new THREE.Vector2(p[0],p[2]));for(const triangle of THREE.ShapeUtils.triangulateShape(cap,[]))indices.push(...triangle.map(i=>n+i));surface(v,indices,color);}
 function rect(x,z,w,d){return [[x-w/2,z-d/2],[x+w/2,z-d/2],[x+w/2,z+d/2],[x-w/2,z+d/2]];}
 // A single polygon extrusion preserves corners and holes, rather than four generic boxes.
 volume(m.outer,m.holes,h,.4,b.category==='住宿'?'#d3c7b9':'#cfc6b7');
 for(const hole of m.holes)volume(hole,[],.15,.15,'#8d9b78');
 if(m.roof==='traced'){
  // Base roof uses the SAME polygon and cutouts as the walls, preserving recesses.
  volume(m.outer,m.holes,.3,h+.4,'#73777b');
  for(const wing of m.roofWings)hipRoof(wing,h+.7,1.6);
  for(const link of m.links){volume(link.outline,[],link.height,.4,'#ded8cd');volume(link.outline,[],.35,link.height+.4,'#e5e2d9');}
 }else if(m.roof==='ring')ringRoof(m.outer,m.holes[0],h+.4,2.0);
 else if(m.roof==='open'){
  for(const wing of m.roofWings)hipRoof(wing,h+.4,1.6);
 }else if(m.roof==='arena'){
  if(m.arenaCenter){
   for(const loop of m.arenaShoulders){const v=loop.map(p=>[p[0],h+(m.arenaCenter.some(c=>Math.hypot(c[0]-p[0],c[1]-p[1])<1e-6)?5:.4),p[1]]);const faces=THREE.ShapeUtils.triangulateShape(loop.map(p=>new THREE.Vector2(p[0],p[1])),[]).flat();surface(v,faces,'#848b8e');}
   volume(m.arenaCenter,[],.4,h+5,'#ba806b');
   const center=[0,1].map(k=>m.arenaCenter.reduce((sum,p)=>sum+p[k],0)/4);
   const court=m.arenaCenter.map(p=>[center[0]+(p[0]-center[0])*.62,center[1]+(p[1]-center[1])*.7]);volume(court,[],.05,h+5.4,'#769ab7');
  }else{

  const outer=m.outer,center=[0,1].map(k=>outer.reduce((sum,p)=>sum+p[k],0)/outer.length);
  const inner=outer.map(p=>[center[0]+(p[0]-center[0])*.58,center[1]+(p[1]-center[1])*.78]);
  const v=outer.map(p=>[p[0],h+.4,p[1]]).concat(inner.map(p=>[p[0],h+5,p[1]])),faces=[],n=outer.length;
  for(let i=0;i<n;i++){const j=(i+1)%n;faces.push(i,j,j+n,i,j+n,i+n);}surface(v,faces,'#7e8588');
  volume(inner,[],.4,h+5,'#e4e7e4');

  }
 }else if(m.roof==='terrace'){
  volume(m.outer,[],.65,h+.4,'#c8c2ba');
  // L-shaped roof parapet and stepped lake-facing terrace.
  const outer=m.outer;for(let i=0;i<outer.length;i++){const a=outer[i],c=outer[(i+1)%outer.length],dx=c[0]-a[0],dz=c[1]-a[1];const rail=box(.35,.6,Math.hypot(dx,dz),(a[0]+c[0])/2,h+1.05,(a[1]+c[1])/2,'#ece7df',g);rail.rotation.y=Math.atan2(dx,dz);}
  box(4,1.1,5,0,h+1.05,-10,'#ddd8cb',g);
  const terrace=m.terraceOutline||m.outer.slice(0,4);
  for(let level=0;level<3;level++){const extended=terrace.map(p=>[p[0]+level*.6,p[1]-level*.4]);volume(extended,[],.4,1.3-level*.35,'#b4b0a8');}
 }else if(m.roof==='hall'){
  hipRoof(m.outer,h+.4,2.1);
 }else if(m.roof==='canteen'){
  hipRoof(m.outer,h+.4,3.2);
 }else hipRoof(m.outer,h+.4,1.8);
 for(const a of [...m.infill,...m.annex]){
  const r=a.outline||rect(a.x,a.z,a.w,a.d);volume(r,[],a.h,.4,'#c9bfb1');
  if(a.roof==='flat')volume(r,[],.5,a.h+.4,'#c6c3bd');else hipRoof(r,a.h+.4,2.2);

 }
 for(const [x,z]of m.corners){box(5.5,1.1,5.5,x,h+.5,z,'#b9a28b',g);hipRoof(rect(x,z,6.5,6.5),h+1.6,1.8,'#b98d6b');}
 // Glazing follows every external and courtyard segment, including angled facades.
 const matrices=[];const dummy=new THREE.Object3D();
 for(const [loopIndex,loop]of [m.outer,...m.holes].entries())for(let i=0;i<loop.length;i++){
  const a=loop[i],c=loop[(i+1)%loop.length],dx=c[0]-a[0],dz=c[1]-a[1],length=Math.hypot(dx,dz),angle=Math.atan2(dx,dz);
  const count=Math.floor(length/3.1);const glass=b.id==='library'&&m.glass.includes(i);
  for(let f=0;f<b.floors;f++){
   if(glass){dummy.position.set((a[0]+c[0])/2,1.8+f*3.2,(a[1]+c[1])/2);dummy.scale.set(.2,2.2,length-1);dummy.rotation.y=angle;dummy.updateMatrix();matrices.push(dummy.matrix.clone());}
   else for(let k=0;k<count;k++){const t=(k+.5)/count;dummy.position.set(a[0]+dx*t,1.8+f*3.2,a[1]+dz*t);dummy.scale.set(.2,1.5,1.6);dummy.rotation.y=angle;dummy.updateMatrix();matrices.push(dummy.matrix.clone());}
  }
 }
 if(matrices.length){const windows=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),mat('#647d84'),matrices.length);matrices.forEach((matrix,i)=>windows.setMatrixAt(i,matrix));windows.userData.id=b.id;g.add(windows);pickables.push(windows);}
 return m;
};
})();
