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
buildCampusGround({scene,flat,box,mat});
const groups=new Map(),pickables=[],labels=[];
const allPlaces=[...CampusData.buildings,...(CampusData.regions||[])],regionGroups=window.SpecialTrace?SpecialTrace.render(CampusData,{scene,flat,mat}):[];
for(const record of regionGroups){pickables.push(record.surface);labels.push({b:record.b,s:label(record.b.name,record.b.x,3,record.b.z,record.b)});}
function label(text,x,y,z,place){
 const el=document.createElement('div');
 el.className='map-label';
 el.textContent=text;
 if(place)el.onclick=e=>{e.stopPropagation();select(place);};
 document.getElementById('labels').append(el);
 return {el,position:new THREE.Vector3(x,y,z),visible:true};
}
function pitchedRoof(w,d,rise,x,y,z,g,id){const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(SceneUtils.roofVertices(w,d,rise),3));geo.setIndex([0,2,1,3,4,5,0,3,5,0,5,2,2,5,4,2,4,1,0,1,4,0,4,3]);geo.computeVertexNormals();const m=new THREE.Mesh(geo,mat('#616c73'));m.position.set(x,y,z);m.castShadow=true;m.receiveShadow=true;m.userData.id=id;g.add(m);pickables.push(m);}
for(const b of CampusData.buildings){
 const g=new THREE.Group();g.position.set(b.x,0,b.z);scene.add(g);
 buildPhotoModel(b,g,{mat,box,pickables});
 const s=label(b.name,b.x,b.floors*3.2+10,b.z,b);labels.push({b,s});groups.set(b.id,g);
}
let theta=.42,phi=.85,radius=560,target=new THREE.Vector3(165,0,155),selected=null,filter='全部',paused=false;
let isCruising=false,cruiseTime=0;
let flyAnimation=null;

function updateCamera(){
 camera.position.set(target.x+radius*Math.sin(phi)*Math.sin(theta),target.y+radius*Math.cos(phi),target.z+radius*Math.sin(phi)*Math.cos(theta));
 camera.lookAt(target);
}

function reset(){
 stopCruise();
 selected=null;
 const card=document.getElementById('card');
 card.classList.remove('selected');
 document.getElementById('title').textContent='晴川 3D 校园';
 document.getElementById('info').textContent='独立建筑模型 · 真实航拍底图 · 情缘湖与运动场';
 document.getElementById('badge').style.display='none';
 document.getElementById('thumb').style.display='none';
 document.getElementById('desc').style.display='none';
 document.getElementById('floors').style.display='none';
 document.getElementById('actions').style.display='none';
 const a=document.getElementById('photo');a.hidden=true;
 for(const g of groups.values())g.traverse(m=>{if(m.isMesh)m.material.emissive.set('#000000');});
 for(const r of regionGroups)r.surface.material.emissive.set('#000000');
 theta=0;phi=.7;radius=Math.max(500,205/Math.tan(42*Math.PI/360)/(innerWidth/innerHeight));
 target.set(165,0,157);
 updateCamera();
}
reset();

function select(b){
 stopCruise();
 document.getElementById('card').classList.add('selected');
 selected=b.id;
 for(const [id,g] of groups)g.traverse(m=>{if(m.isMesh)m.material.emissive.set(id===b.id?'#344c12':'#000000');});
 for(const r of regionGroups)r.surface.material.emissive.set(r.b.id===b.id?'#344c12':'#000000');
 target.set(b.x,0,b.z);
 radius=Math.min(radius,b.region?190:230);
 phi=.85;
 updateCamera();

 const lm=(window.CampusLandmarks&&window.CampusLandmarks[b.id])||null;
 const titleEl=document.getElementById('title');
 const badgeEl=document.getElementById('badge');
 const infoEl=document.getElementById('info');
 const descEl=document.getElementById('desc');
 const floorsEl=document.getElementById('floors');
 const thumbEl=document.getElementById('thumb');
 const actionsEl=document.getElementById('actions');
 const photoA=document.getElementById('photo');

 titleEl.textContent=lm?lm.fullName:b.name;
 if(lm&&lm.category){badgeEl.textContent=lm.category;badgeEl.style.display='inline-block';}
 else if(b.category){badgeEl.textContent=b.category+'区';badgeEl.style.display='inline-block';}
 else badgeEl.style.display='none';

 infoEl.textContent=lm?(lm.totalFloors+' · '+(lm.tag||'')): (b.info||('共'+b.floors+'层 · '+b.category+'区'));

 if(lm&&lm.desc){descEl.textContent=lm.desc;descEl.style.display='block';}
 else descEl.style.display='none';

 if(lm&&lm.floorsGuide){floorsEl.textContent=lm.floorsGuide;floorsEl.style.display='block';}
 else floorsEl.style.display='none';

 const photoPath=lm?lm.photo:('photos/'+b.id+'.jpg');
 thumbEl.src=photoPath;
 thumbEl.style.display='block';
 thumbEl.onerror=function(){this.style.display='none';};

 actionsEl.style.display='flex';
 if(b.region){photoA.hidden=true;}
 else{
  photoA.hidden=false;
  photoA.href='campus://detail/'+encodeURIComponent(b.id);
 }
}

// Fly to current selected building
document.getElementById('fly').onclick=function(){
 const b=allPlaces.find(p=>p.id===selected);
 if(!b)return;
 stopCruise();
 const startPos=camera.position.clone();
 const endPos=new THREE.Vector3(b.x,18,b.z+42);
 const startLook=target.clone();
 const endLook=new THREE.Vector3(b.x,b.floors?b.floors*1.6:5,b.z);
 const startTime=performance.now(),duration=1500;
 flyAnimation={
  update:(now)=>{
   const t=Math.min(1,(now-startTime)/duration);
   const ease=t<.5?2*t*t:-1+(4-2*t)*t;
   camera.position.lerpVectors(startPos,endPos,ease);
   target.lerpVectors(startLook,endLook,ease);
   camera.lookAt(target);
   if(t>=1)flyAnimation=null;
  }
 };
};

document.getElementById('close').onclick=reset;

const raycaster=new THREE.Raycaster();
function pick(x,y){
 raycaster.setFromCamera(new THREE.Vector2(x/innerWidth*2-1,1-y/innerHeight*2),camera);
 const hit=raycaster.intersectObjects(pickables).find(h=>h.object.parent.visible);
 if(hit){
  const b=allPlaces.find(b=>b.id===hit.object.userData.id);
  if(b)select(b);
 }
}

for(const name of ['全部','教学','住宿','生活','文体']){
 const btn=document.createElement('button');
 btn.textContent=name;
 btn.className=name==='全部'?'active':'';
 btn.onclick=()=>{
  filter=name;
  for(const [id,g]of groups)g.visible=name==='全部'||CampusData.buildings.find(b=>b.id===id).category===name;
  for(const r of regionGroups)r.group.visible=name==='全部'||r.b.category===name;
  for(const {b,s}of labels)s.visible=name==='全部'||b.category===name;
  document.querySelectorAll('#filters button').forEach(el=>el.classList.toggle('active',el===btn));
 };
 document.getElementById('filters').append(btn);
}

function search(){
 const q=document.getElementById('search').value.trim().replace('教1','教学楼1').replace('教2','教学楼2').replace('教3','教学楼3').replace('教4','教学楼4').replace(/^宿舍(\d+)$/,'$1号宿舍');
 const b=allPlaces.find(b=>q&&(b.name.includes(q)||b.id===q||(b.name&&q.includes(b.name))));
 if(b){
  document.querySelector('#filters button').click();
  select(b);
 }else document.getElementById('info').textContent='未找到地点，请输入建筑或区域名称。';
}
document.getElementById('find').onclick=search;
document.getElementById('search').onkeydown=e=>{if(e.key==='Enter')search();};

document.getElementById('plus').onclick=()=>{stopCruise();radius=Math.max(65,radius*.8);updateCamera();};
document.getElementById('minus').onclick=()=>{stopCruise();radius=Math.min(1100,radius*1.25);updateCamera();};
document.getElementById('reset').onclick=reset;
document.getElementById('top').onclick=()=>{stopCruise();phi=phi<.1?.85:.025;updateCamera();};

// Cruise mode implementation
const cruiseBtn=document.getElementById('cruise');
const cruiseWaypoints=[
 new THREE.Vector3(204.5,14,310), // South Gate entrance
 new THREE.Vector3(204.5,20,240), // Central lawn / Flagpole
 new THREE.Vector3(180,18,180),   // Lake south bank
 new THREE.Vector3(135,22,145),   // Library curved terrace
 new THREE.Vector3(160,26,95),    // Athletic Stadium & Gym
 new THREE.Vector3(240,28,160),   // Teaching buildings 1-4
 new THREE.Vector3(215,45,285)    // High overview climb
];
const cruiseLooks=[
 new THREE.Vector3(204.5,6,250),
 new THREE.Vector3(180,8,170),
 new THREE.Vector3(140,6,150),
 new THREE.Vector3(124,10,152),
 new THREE.Vector3(160,6,66),
 new THREE.Vector3(265,12,210),
 new THREE.Vector3(165,0,155)
];
const posCurve=new THREE.CatmullRomCurve3(cruiseWaypoints,true,'centripetal');
const lookCurve=new THREE.CatmullRomCurve3(cruiseLooks,true,'centripetal');

function startCruise(){
 isCruising=true;
 cruiseTime=0;
 flyAnimation=null;
 cruiseBtn.classList.add('active');
 cruiseBtn.textContent='漫游中';
 document.getElementById('info').textContent='正在沿晴川中轴线与核心景观自动巡航漫游... 点击任意处退出';
}
function stopCruise(){
 if(!isCruising)return;
 isCruising=false;
 cruiseBtn.classList.remove('active');
 cruiseBtn.textContent='巡航';
}
cruiseBtn.onclick=function(){
 if(isCruising)stopCruise();else startCruise();
};

let currentGroundMode='schematic';
const modeBtn=document.getElementById('mode');
function toggleGroundMode(){
 currentGroundMode=currentGroundMode==='schematic'?'aerial':'schematic';
 if(window.setGroundMode)window.setGroundMode(currentGroundMode);
 modeBtn.textContent=currentGroundMode==='aerial'?'实景':'沙盘';
 modeBtn.classList.toggle('active',currentGroundMode==='aerial');
}
if(modeBtn){
 modeBtn.textContent='沙盘';
 modeBtn.addEventListener('click',toggleGroundMode);
 modeBtn.addEventListener('touchend',e=>{e.preventDefault();toggleGroundMode();});
}

const pointers=new Map();let down=null,moved=false,pinch=0;
const canvas=renderer.domElement;
canvas.onpointerdown=e=>{
 stopCruise();
 flyAnimation=null;
 canvas.setPointerCapture(e.pointerId);
 pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});
 if(pointers.size===1){down={x:e.clientX,y:e.clientY};moved=false;}
 else{moved=true;pinch=0;}
};
canvas.onpointermove=e=>{
 const old=pointers.get(e.pointerId);
 if(!old)return;
 const dx=e.clientX-old.x,dy=e.clientY-old.y;
 pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});
 if(down&&Math.hypot(e.clientX-down.x,e.clientY-down.y)>7)moved=true;
 if(pointers.size===1){
  if(window.RoadEditor&&window.RoadEditor.isEditing&&window.RoadEditor.isEditing()){
   if(window.RoadEditor.getMode&&window.RoadEditor.getMode()==='pan'){
    const panK=(radius*0.0016);
    target.x-=dx*panK*Math.cos(theta);
    target.z+=dx*panK*Math.sin(theta);
    target.z-=dy*panK*Math.cos(theta);
    target.x-=dy*panK*Math.sin(theta);
    target.x=THREE.MathUtils.clamp(target.x,-50,380);
    target.z=THREE.MathUtils.clamp(target.z,-50,380);
    updateCamera();
   }
   return;
  }
  theta-=dx*.007;
  phi=THREE.MathUtils.clamp(phi+dy*.005,.025,1.4);
 }else{
  const [a,b]=[...pointers.values()],dist=Math.hypot(a.x-b.x,a.y-b.y);
  if(pinch)radius=THREE.MathUtils.clamp(radius*pinch/Math.max(dist,1),65,1100);
  pinch=dist;
  const k=radius*.001;
  target.x-=dx*k*Math.cos(theta);
  target.z+=dx*k*Math.sin(theta);
  target.z-=dy*k*Math.cos(theta);
  target.x-=dy*k*Math.sin(theta);
  target.x=THREE.MathUtils.clamp(target.x,-30,330);
  target.z=THREE.MathUtils.clamp(target.z,-30,390);
 }
 updateCamera();
};
function release(e){
 if(window.RoadEditor&&window.RoadEditor.isEditing&&window.RoadEditor.isEditing()){
  pointers.delete(e.pointerId);pinch=0;down=null;
  return;
 }
 if(!moved&&pointers.size===1&&down&&e.type!=='pointercancel')pick(e.clientX,e.clientY);
 pointers.delete(e.pointerId);pinch=0;down=null;
}
canvas.onpointerup=release;
canvas.onpointercancel=release;
canvas.onwheel=e=>{
 stopCruise();
 flyAnimation=null;
 e.preventDefault();
 radius=THREE.MathUtils.clamp(radius*Math.exp(e.deltaY*.001),65,1100);
 updateCamera();
};
window.addEventListener('load',()=>requestAnimationFrame(reset));
window.onresize=()=>{
 camera.aspect=innerWidth/innerHeight;
 camera.updateProjectionMatrix();
 renderer.setSize(innerWidth,innerHeight);
};
document.addEventListener('visibilitychange',()=>paused=document.hidden);
window.resetCameraTop=function(){
 stopCruise();
 selected=null;
 target.set(165,0,157.5);
 phi=0.025;
 theta=0;
 radius=480;
 updateCamera();
};
if(window.RoadEditor&&typeof window.RoadEditor.init==='function'){
 window.RoadEditor.init({scene,camera,renderer});
}
window.CampusScene={
 select:id=>{const b=allPlaces.find(b=>b.id===id);if(b)select(b);},
 pause:v=>paused=v,
 buildings:CampusData.buildings,
 regions:CampusData.regions||[]
};

function updateLabels(){
 const candidates=[];const close=radius<480;
 const top=document.getElementById('filters').getBoundingClientRect().bottom+8;
 const bottom=document.querySelector('.card').getBoundingClientRect().top-10;
 const reserved=['.badge','.tools'].map(q=>document.querySelector(q).getBoundingClientRect());
 for(const {b,s}of labels){
  s.el.style.display='none';
  if(!s.visible)continue;
  const important=b.region||['library','gym','canteen_main','teach_1','theater_market'].includes(b.id);
  if(!close&&!important&&b.id!==selected)continue;
  const p=s.position.clone().project(camera);
  if(p.z<-1||p.z>1)continue;
  const x=(p.x+1)*innerWidth/2,y=(1-p.y)*innerHeight/2;
  const w=Math.max(46,b.name.length*11+14),h=24;
  if(x-w/2<5||x+w/2>innerWidth-5||y<top+h||y>bottom)continue;
  if(reserved.some(r=>x+w/2>r.left-4&&x-w/2<r.right+4&&y>r.top-4&&y-h<r.bottom+4))continue;
  candidates.push({id:b.id,priority:b.id===selected?100:important?10:1,rect:[x-w/2,y-h,x+w/2,y],x,y,s});
 }
 const accepted=new Set(SceneUtils.chooseLabels(candidates));
 for(const c of candidates)if(accepted.has(c.id)){
  c.s.el.style.display='block';
  c.s.el.style.left=c.x+'px';
  c.s.el.style.top=c.y+'px';
  c.s.el.classList.toggle('chosen',c.id===selected);
 }
 document.getElementById('compass').style.transform='rotate('+(-theta)+'rad)';
}

let lastFrame=0;
function frame(now){
 requestAnimationFrame(frame);
 if(paused)return;
 if(flyAnimation)flyAnimation.update(now);
 else if(isCruising){
  cruiseTime+=0.0006;
  const t=(cruiseTime%1);
  const p=posCurve.getPointAt(t);
  const l=lookCurve.getPointAt(t);
  camera.position.copy(p);
  camera.lookAt(l);
  target.copy(l);
 }
 if(now-lastFrame>32){
  lastFrame=now;
  renderer.render(scene,camera);
  updateLabels();
 }
}
requestAnimationFrame(frame);
})();
