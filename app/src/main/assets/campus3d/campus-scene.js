/* Local, approximate exterior reconstruction; no geographic or indoor navigation claims. */
(function(){'use strict';
const error=document.getElementById('error');
window.addEventListener('error',e=>{error.style.display='block';error.textContent='三维场景加载失败：'+e.message+'。请返回航拍导览。';});
const scene=new THREE.Scene();scene.background=new THREE.Color('#e7efec');scene.fog=new THREE.Fog('#e7efec',1500,2800);
const renderer=new THREE.WebGLRenderer({antialias:true});renderer.setPixelRatio(Math.min(devicePixelRatio,1.6));renderer.setSize(innerWidth,innerHeight);renderer.shadowMap.enabled=true;renderer.shadowMap.type=THREE.PCFSoftShadowMap;renderer.outputColorSpace=THREE.SRGBColorSpace;document.body.prepend(renderer.domElement);
const camera=new THREE.PerspectiveCamera(42,innerWidth/innerHeight,1,1800);scene.add(new THREE.HemisphereLight(0xffffff,0x73816d,1.5));
const sun=new THREE.DirectionalLight(0xfff4dc,2.0);sun.position.set(-200,450,180);sun.castShadow=true;sun.shadow.mapSize.set(1024,1024);Object.assign(sun.shadow.camera,{left:-360,right:360,top:400,bottom:-400,far:1000});sun.shadow.bias=-.001;scene.add(sun);
const materials=new Map();function mat(color){if(!materials.has(color))materials.set(color,new THREE.MeshStandardMaterial({color,roughness:.86}));return materials.get(color).clone();}
function box(w,h,d,x,y,z,color,parent=scene){if(color==='#789a9c'){if(!parent.userData.windows)parent.userData.windows=[];parent.userData.windows.push([w,h,d,x,y+h/2,z]);return;}const m=new THREE.Mesh(new THREE.BoxGeometry(w,h,d),mat(color));m.position.set(x,y+h/2,z);m.castShadow=true;m.receiveShadow=true;parent.add(m);return m;}
function flat(points,y,color,parent=scene){const s=new THREE.Shape();points.forEach((p,i)=>i?s.lineTo(p[0],-p[1]):s.moveTo(p[0],-p[1]));s.closePath();const m=new THREE.Mesh(new THREE.ShapeGeometry(s),mat(color));m.rotation.x=-Math.PI/2;m.position.y=y;m.receiveShadow=true;parent.add(m);return m;}
box(340,7,395,150,-8,180,'#cad6bc');
const boundary=[[12,40],[95,30],[204,16],[299,20],[304,333],[165,347],[12,329]];
flat(boundary,.01,'#9bb58c');
function road(points,width=6,color='#e2d9c7',y=.18){for(let i=1;i<points.length;i++){const [a,b]=[points[i-1],points[i]],dx=b[0]-a[0],dz=b[1]-a[1];const m=box(width,.12,Math.hypot(dx,dz), (a[0]+b[0])/2,y,(a[1]+b[1])/2,color);m.rotation.y=Math.atan2(dx,dz);}}
road([[15,46],[97,34],[207,23],[298,25],[299,329],[174,337],[14,324],[15,46]],8);
road([[65,47],[65,324]],6);road([[105,44],[101,122],[99,222],[98,325]],7);
road([[184,27],[189,109],[198,139],[190,219],[180,325]],7);
road([[16,146],[95,146],[112,129],[169,130],[203,129],[298,129]],6);
road([[14,233],[92,233],[156,232],[188,229],[298,229]],6);
road([[15,324],[298,324]],7);road([[215,24],[215,120]],4);road([[255,24],[255,120]],4);
road([[216,74],[292,74]],4);road([[221,228],[221,325]],4);road([[181,278],[292,278]],5);
for(const z of [101,145,186,227,270,310])road([[15,z],[97,z]],3);
flat([[125,136],[157,127],[181,145],[185,169],[179,193],[164,212],[136,216],[121,197],[116,167]],.24,'#257f86');
road([[123,134],[155,124],[184,140],[190,169],[184,199],[164,219],[133,223],[113,199],[109,164],[123,134]],2.6,'#e7e1d1',.35);
flat([[204,141],[290,141],[290,219],[197,219]],.2,'#a8bf91');road([[244,137],[244,225]],3);road([[195,181],[291,181]],3);
// Track, football pitch and court markings.
function rounded(w,d,r){const s=new THREE.Shape();s.moveTo(-w/2+r,-d/2);s.lineTo(w/2-r,-d/2);s.quadraticCurveTo(w/2,-d/2,w/2,-d/2+r);s.lineTo(w/2,d/2-r);s.quadraticCurveTo(w/2,d/2,w/2-r,d/2);s.lineTo(-w/2+r,d/2);s.quadraticCurveTo(-w/2,d/2,-w/2,d/2-r);s.lineTo(-w/2,-d/2+r);s.quadraticCurveTo(-w/2,-d/2,-w/2+r,-d/2);return s;}
const track=new THREE.Mesh(new THREE.ShapeGeometry(rounded(53,92,22)),mat('#b96e61'));track.rotation.x=-Math.PI/2;track.position.set(169,.4,77);scene.add(track);
for(let i=0;i<4;i++){const points=rounded(51-i*2.4,90-i*2.4,21-i*1.2).getPoints(60).map(p=>new THREE.Vector3(p.x,.46,-p.y));points.push(points[0]);const l=new THREE.Line(new THREE.BufferGeometry().setFromPoints(points),new THREE.LineBasicMaterial({color:'#e8baab'}));l.position.set(169,0,77);scene.add(l);}
box(34,.12,60,169,.48,77,'#528a67');
function line(points,color='#faf8ee'){const l=new THREE.Line(new THREE.BufferGeometry().setFromPoints(points.map(p=>new THREE.Vector3(p[0],.7,p[1]))),new THREE.LineBasicMaterial({color}));scene.add(l);}
line([[153,48],[185,48],[185,106],[153,106],[153,48]]);line([[153,77],[185,77]]);
const circle=[];for(let i=0;i<=48;i++)circle.push([169+6*Math.cos(i*Math.PI/24),77+6*Math.sin(i*Math.PI/24)]);line(circle);
for(const [x,z] of [[24,192],[24,214],[128,250],[151,250],[106,333]]){box(17,.15,18,x,.35,z,'#6c9b9a');line([[x-7,z-8],[x+7,z-8],[x+7,z+8],[x-7,z+8],[x-7,z-8]]);line([[x-7,z],[x+7,z]]);}
const groups=new Map(),pickables=[],labels=[];
function label(text,x,y,z){const el=document.createElement('div');el.className='map-label';el.textContent=text;document.getElementById('labels').append(el);return {el,position:new THREE.Vector3(x,y,z),visible:true};}
function pitchedRoof(w,d,rise,x,y,z,g,id){const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(SceneUtils.roofVertices(w,d,rise),3));geo.setIndex([0,2,1,3,4,5,0,3,5,0,5,2,2,5,4,2,4,1,0,1,4,0,4,3]);geo.computeVertexNormals();const m=new THREE.Mesh(geo,mat('#616c73'));m.position.set(x,y,z);m.castShadow=true;m.receiveShadow=true;m.userData.id=id;g.add(m);pickables.push(m);}
for(const b of CampusData.buildings){
 const g=new THREE.Group();g.position.set(b.x,0,b.z);scene.add(g);
 buildPhotoModel(b,g,{mat,box,pickables});
 const s=label(b.name,b.x,b.floors*3.2+10,b.z);labels.push({b,s});groups.set(b.id,g);
}
// Connections visible in the teaching-area photographs; not entrance navigation.
box(12,2.5,4,223.5,8,255,'#cfc5b5');box(12,2.5,4,223.5,8,302,'#cfc5b5');
// Trees along avenues, with shared geometry/materials.
const trunkGeo=new THREE.CylinderGeometry(.35,.5,2,5),leafGeo=new THREE.IcosahedronGeometry(2.2,0),trunkMat=mat('#8d7b61'),leafMat=mat('#628b63');
function tree(x,z){const t=new THREE.Mesh(trunkGeo,trunkMat);t.position.set(x,1,z);scene.add(t);const l=new THREE.Mesh(leafGeo,leafMat);l.position.set(x,4,z);l.castShadow=true;scene.add(l);}
for(let z=45;z<327;z+=11){tree(9,z);tree(307,z);if(z>225)tree(175,z);}
for(let x=110;x<295;x+=12){tree(x,224);tree(x,332);}
let theta=.42,phi=.85,radius=560,target=new THREE.Vector3(150,0,180),selected=null,filter='全部',paused=false;
function updateCamera(){camera.position.set(target.x+radius*Math.sin(phi)*Math.sin(theta),target.y+radius*Math.cos(phi),target.z+radius*Math.sin(phi)*Math.cos(theta));camera.lookAt(target);}
function reset(){selected=null;document.querySelector('.card').classList.remove('selected');document.getElementById('title').textContent='晴川 3D 校园';document.getElementById('info').textContent='拖动旋转 · 双指缩放 · 点击建筑查看楼层';document.getElementById('photo').hidden=true;for(const g of groups.values())g.traverse(m=>{if(m.isMesh)m.material.emissive.set('#000000');});theta=.25;phi=.85;radius=Math.max(500,250/Math.tan(42*Math.PI/360)/(innerWidth/innerHeight));target.set(150,0,180);updateCamera();}reset();
function select(b){document.querySelector('.card').classList.add('selected');selected=b.id;for(const [id,g] of groups)g.traverse(m=>{if(m.isMesh)m.material.emissive.set(id===b.id?'#344c12':'#000000');});target.set(b.x,0,b.z);radius=Math.min(radius,245);phi=.85;updateCamera();document.getElementById('title').textContent=b.name;document.getElementById('info').textContent=b.info||('共'+b.floors+'层 · '+b.category+'区');document.getElementById('sub').textContent='外观与尺寸为截图近似重建，暂无入口或室内导航。';const a=document.getElementById('photo');a.hidden=false;a.href='campus://detail/'+encodeURIComponent(b.id);}
const raycaster=new THREE.Raycaster();function pick(x,y){raycaster.setFromCamera(new THREE.Vector2(x/innerWidth*2-1,1-y/innerHeight*2),camera);const hit=raycaster.intersectObjects(pickables).find(h=>h.object.parent.visible);if(hit)select(CampusData.buildings.find(b=>b.id===hit.object.userData.id));}
for(const name of ['全部','教学','住宿','生活','文体']){const btn=document.createElement('button');btn.textContent=name;btn.className=name==='全部'?'active':'';btn.onclick=()=>{filter=name;for(const [id,g]of groups)g.visible=name==='全部'||CampusData.buildings.find(b=>b.id===id).category===name;for(const {b,s}of labels)s.visible=name==='全部'||b.category===name;document.querySelectorAll('#filters button').forEach(el=>el.classList.toggle('active',el===btn));};document.getElementById('filters').append(btn);}
function search(){const q=document.getElementById('search').value.trim().replace('教1','教学楼1').replace('教2','教学楼2').replace('教3','教学楼3').replace('教4','教学楼4').replace(/^宿舍(\d+)$/,'$1号宿舍');const b=CampusData.buildings.find(b=>q&&(b.name.includes(q)||b.id===q));if(b){document.querySelector('#filters button').click();select(b);}else document.getElementById('info').textContent='未找到建筑，请输入教学楼、图书馆或宿舍编号。';}
document.getElementById('find').onclick=search;document.getElementById('search').onkeydown=e=>{if(e.key==='Enter')search();};
document.getElementById('plus').onclick=()=>{radius=Math.max(65,radius*.8);updateCamera();};document.getElementById('minus').onclick=()=>{radius=Math.min(1100,radius*1.25);updateCamera();};document.getElementById('reset').onclick=reset;document.getElementById('top').onclick=()=>{phi=phi<.1?.85:.025;updateCamera();};
const pointers=new Map();let down=null,moved=false,pinch=0;
const canvas=renderer.domElement;canvas.onpointerdown=e=>{canvas.setPointerCapture(e.pointerId);pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});if(pointers.size===1){down={x:e.clientX,y:e.clientY};moved=false;}else{moved=true;pinch=0;}};
canvas.onpointermove=e=>{const old=pointers.get(e.pointerId);if(!old)return;const dx=e.clientX-old.x,dy=e.clientY-old.y;pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});if(down&&Math.hypot(e.clientX-down.x,e.clientY-down.y)>7)moved=true;
 if(pointers.size===1){theta-=dx*.007;phi=THREE.MathUtils.clamp(phi+dy*.005,.025,1.4);}else{const [a,b]=[...pointers.values()],dist=Math.hypot(a.x-b.x,a.y-b.y);if(pinch)radius=THREE.MathUtils.clamp(radius*pinch/Math.max(dist,1),65,1100);pinch=dist;const k=radius*.001;target.x-=dx*k*Math.cos(theta);target.z+=dx*k*Math.sin(theta);target.z-=dy*k*Math.cos(theta);target.x-=dy*k*Math.sin(theta);target.x=THREE.MathUtils.clamp(target.x,-30,330);target.z=THREE.MathUtils.clamp(target.z,-30,390);}updateCamera();};
function release(e){if(!moved&&pointers.size===1&&down&&e.type!=='pointercancel')pick(e.clientX,e.clientY);pointers.delete(e.pointerId);pinch=0;down=null;}canvas.onpointerup=release;canvas.onpointercancel=release;canvas.onwheel=e=>{e.preventDefault();radius=THREE.MathUtils.clamp(radius*Math.exp(e.deltaY*.001),65,1100);updateCamera();};
window.addEventListener('load',()=>requestAnimationFrame(reset));
window.onresize=()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight);};
document.addEventListener('visibilitychange',()=>paused=document.hidden);window.CampusScene={select:id=>{const b=CampusData.buildings.find(b=>b.id===id);if(b)select(b);},pause:v=>paused=v,buildings:CampusData.buildings};
function updateLabels(){
 const candidates=[];const close=radius<480;
 const top=document.getElementById('filters').getBoundingClientRect().bottom+8;
 const bottom=document.querySelector('.card').getBoundingClientRect().top-10;
 const reserved=['.badge','.tools'].map(q=>document.querySelector(q).getBoundingClientRect());
 for(const {b,s}of labels){s.el.style.display='none';if(!s.visible)continue;
  const important=['library','gym','canteen_main','teach_1','theater_market'].includes(b.id);
  if(!close&&!important&&b.id!==selected)continue;
  const p=s.position.clone().project(camera);if(p.z<-1||p.z>1)continue;
  const x=(p.x+1)*innerWidth/2,y=(1-p.y)*innerHeight/2;
  const w=Math.max(46,b.name.length*11+14),h=24;
  if(x-w/2<5||x+w/2>innerWidth-5||y<top+h||y>bottom)continue;
  if(reserved.some(r=>x+w/2>r.left-4&&x-w/2<r.right+4&&y>r.top-4&&y-h<r.bottom+4))continue;
  candidates.push({id:b.id,priority:b.id===selected?100:important?10:1,rect:[x-w/2,y-h,x+w/2,y],x,y,s});
 }
 const accepted=new Set(SceneUtils.chooseLabels(candidates));
 for(const c of candidates)if(accepted.has(c.id)){c.s.el.style.display='block';c.s.el.style.left=c.x+'px';c.s.el.style.top=c.y+'px';c.s.el.classList.toggle('chosen',c.id===selected);}
 document.getElementById('compass').style.transform='rotate('+(-theta)+'rad)';
}
let lastFrame=0;function frame(now){requestAnimationFrame(frame);if(!paused&&now-lastFrame>32){lastFrame=now;renderer.render(scene,camera);updateLabels();}}requestAnimationFrame(frame);
})();
