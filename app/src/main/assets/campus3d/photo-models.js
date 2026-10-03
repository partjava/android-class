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
 function hipRoof(outer,y,rise,color='#686970'){const cx=outer.reduce((s,p)=>s+p[0],0)/outer.length,cz=outer.reduce((s,p)=>s+p[1],0)/outer.length;const v=[];for(const p of outer)v.push([p[0],y,p[1]]);for(const p of outer)v.push([cx+(p[0]-cx)*.40,y+rise,cz+(p[1]-cz)*.65]);const indices=[],n=outer.length;for(let i=0;i<n;i++){const j=(i+1)%n;indices.push(i,j,j+n,i,j+n,i+n);}for(let i=1;i<n-1;i++)indices.push(n,n+i,n+i+1);surface(v,indices,color);}
 function rect(x,z,w,d){return [[x-w/2,z-d/2],[x+w/2,z-d/2],[x+w/2,z+d/2],[x-w/2,z+d/2]];}
 // A single polygon extrusion preserves corners and holes, rather than four generic boxes.
 volume(m.outer,m.holes,h,.4,b.category==='住宿'?'#d3c7b9':'#cfc6b7');
 for(const hole of m.holes)volume(hole,[],.15,.15,'#8d9b78');
 if(m.roof==='ring')ringRoof(m.outer,m.holes[0],h+.4,2.0);
 else if(m.roof==='open'){
  // Three separate hipped roof wings follow the opening direction.
  const xMin=Math.min(...m.outer.map(p=>p[0])),xMax=Math.max(...m.outer.map(p=>p[0])),zMin=Math.min(...m.outer.map(p=>p[1])),zMax=Math.max(...m.outer.map(p=>p[1]));
  const openSouth=m.outer[1][0]>0;
  hipRoof(rect(xMin+4.4,(zMin+zMax)/2,8.8,zMax-zMin),h+.4,1.6);
  hipRoof(rect(xMax-4.4,(zMin+zMax)/2,8.8,zMax-zMin),h+.4,1.6);
  hipRoof(rect((xMin+xMax)/2,openSouth?zMin+4.4:zMax-4.4,xMax-xMin-17.6,8.8),h+.4,1.6);
 }else if(m.roof==='arena'){
  // Bright central arena roof and broad sloped wings seen in 体育馆.png.
  const v=[[-22,h+.4,-28],[21,h+.4,-28],[21,h+.4,29],[-22,h+.4,29],[-12,h+7,-21],[12,h+7,-21],[12,h+7,21],[-12,h+7,21]];
  surface(v,[0,1,5,0,5,4,1,2,6,1,6,5,2,3,7,2,7,6,3,0,4,3,4,7],'#7e8588');
  surface(v,[4,5,6,4,6,7],'#e4e7e4');
  box(26,2.4,2,0,h+7,-22,'#596c76',g);box(26,2.4,2,0,h+7,22,'#596c76',g);
  box(33,2,.3,0,1,29.1,'#577c80',g);box(26,1,.35,0,3.6,29.2,'#a07161',g);
 }else if(m.roof==='terrace'){
  volume(m.outer,[],.65,h+.4,'#c8c2ba');
  // L-shaped roof parapet and stepped lake-facing terrace.
  const outer=m.outer;for(let i=0;i<outer.length;i++){const a=outer[i],c=outer[(i+1)%outer.length],dx=c[0]-a[0],dz=c[1]-a[1];const rail=box(.35,.6,Math.hypot(dx,dz),(a[0]+c[0])/2,h+1.05,(a[1]+c[1])/2,'#ece7df',g);rail.rotation.y=Math.atan2(dx,dz);}
  box(18,1.1,8,9,h+1.05,-20,'#ddd8cb',g);box(8,.5,5,-16,h+1.05,6,'#ddd8cb',g);
  const terrace=[[-30,27],[-34,16],[-20,-14],[-15,-14],[-26,17],[-24,25]];
  for(let level=0;level<4;level++){const extended=terrace.map(p=>[p[0]-level*1.1,p[1]+level*.45]);volume(extended,[],.5,1.5-level*.35,'#b4b0a8');}
 }else if(m.roof==='hall'){
  hipRoof(m.outer,h+.4,2.1);box(2.5,.35,27,0,h+2.6,0,'#aab8ba',g);
 }else if(m.roof==='canteen'){
  hipRoof(m.outer,h+.4,3.2);surface([[-13,h+3,-13],[10,h+3,-11],[-9,h+3,6]],[0,1,2],'#d6d3c8');
  box(7,1.1,3,-11,h+3.1,-12,'#8f9694',g);
 }else hipRoof(m.outer,h+.4,1.8);
 for(const a of [...m.infill,...m.annex]){
  const r=rect(a.x,a.z,a.w,a.d);volume(r,[],a.h,.4,'#c9bfb1');
  if(a.roof==='flat')volume(r,[],.5,a.h+.4,'#c6c3bd');else hipRoof(r,a.h+.4,2.2);
  if(a.connect)box(10,2.4,4,a.x-9,3.6,a.z+5,'#c9bfb1',g);
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
