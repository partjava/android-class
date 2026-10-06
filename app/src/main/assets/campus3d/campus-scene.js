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
for(const record of regionGroups){pickables.push(record.surface);labels.push({b:record.b,s:label(record.b.name,record.b.x,3,record.b.z)});}
function label(text,x,y,z){const el=document.createElement('div');el.className='map-label';el.textContent=text;document.getElementById('labels').append(el);return {el,position:new THREE.Vector3(x,y,z),visible:true};}
function pitchedRoof(w,d,rise,x,y,z,g,id){const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(SceneUtils.roofVertices(w,d,rise),3));geo.setIndex([0,2,1,3,4,5,0,3,5,0,5,2,2,5,4,2,4,1,0,1,4,0,4,3]);geo.computeVertexNormals();const m=new THREE.Mesh(geo,mat('#616c73'));m.position.set(x,y,z);m.castShadow=true;m.receiveShadow=true;m.userData.id=id;g.add(m);pickables.push(m);}
for(const b of CampusData.buildings){
 const g=new THREE.Group();g.position.set(b.x,0,b.z);scene.add(g);
 buildPhotoModel(b,g,{mat,box,pickables});
 const s=label(b.name,b.x,b.floors*3.2+10,b.z);labels.push({b,s});groups.set(b.id,g);
}
let theta=.42,phi=.85,radius=560,target=new THREE.Vector3(165,0,155),selected=null,filter='全部',paused=false;
function updateCamera(){camera.position.set(target.x+radius*Math.sin(phi)*Math.sin(theta),target.y+radius*Math.cos(phi),target.z+radius*Math.sin(phi)*Math.cos(theta));camera.lookAt(target);}
function reset(){selected=null;document.querySelector('.card').classList.remove('selected');document.getElementById('title').textContent='晴川 3D 校园';document.getElementById('info').textContent='拖动旋转 · 双指缩放 · 点击建筑查看楼层';document.getElementById('photo').hidden=true;for(const g of groups.values())g.traverse(m=>{if(m.isMesh)m.material.emissive.set('#000000');});for(const r of regionGroups)r.surface.material.emissive.set('#000000');theta=0;phi=.7;radius=Math.max(500,205/Math.tan(42*Math.PI/360)/(innerWidth/innerHeight));target.set(165,0,157);updateCamera();}reset();
function select(b){document.querySelector('.card').classList.add('selected');selected=b.id;for(const [id,g] of groups)g.traverse(m=>{if(m.isMesh)m.material.emissive.set(id===b.id?'#344c12':'#000000');});for(const r of regionGroups)r.surface.material.emissive.set(r.b.id===b.id?'#344c12':'#000000');target.set(b.x,0,b.z);radius=Math.min(radius,b.region?200:245);phi=.85;updateCamera();document.getElementById('title').textContent=b.name;document.getElementById('info').textContent=b.info||('共'+b.floors+'层 · '+b.category+'区');document.getElementById('sub').textContent='外观与尺寸为截图近似重建，暂无入口或室内导航。';const a=document.getElementById('photo');a.hidden=!!b.region;if(b.region)a.removeAttribute('href');else a.href='campus://detail/'+encodeURIComponent(b.id);}
const raycaster=new THREE.Raycaster();function pick(x,y){raycaster.setFromCamera(new THREE.Vector2(x/innerWidth*2-1,1-y/innerHeight*2),camera);const hit=raycaster.intersectObjects(pickables).find(h=>h.object.parent.visible);if(hit){const b=allPlaces.find(b=>b.id===hit.object.userData.id);if(b)select(b);}}
for(const name of ['全部','教学','住宿','生活','文体']){const btn=document.createElement('button');btn.textContent=name;btn.className=name==='全部'?'active':'';btn.onclick=()=>{filter=name;for(const [id,g]of groups)g.visible=name==='全部'||CampusData.buildings.find(b=>b.id===id).category===name;for(const r of regionGroups)r.group.visible=name==='全部'||r.b.category===name;for(const {b,s}of labels)s.visible=name==='全部'||b.category===name;document.querySelectorAll('#filters button').forEach(el=>el.classList.toggle('active',el===btn));};document.getElementById('filters').append(btn);}
function search(){const q=document.getElementById('search').value.trim().replace('教1','教学楼1').replace('教2','教学楼2').replace('教3','教学楼3').replace('教4','教学楼4').replace(/^宿舍(\d+)$/,'$1号宿舍');const b=allPlaces.find(b=>q&&(b.name.includes(q)||b.id===q));if(b){document.querySelector('#filters button').click();select(b);}else document.getElementById('info').textContent='未找到地点，请输入建筑或区域名称。';}
document.getElementById('find').onclick=search;document.getElementById('search').onkeydown=e=>{if(e.key==='Enter')search();};
document.getElementById('plus').onclick=()=>{radius=Math.max(65,radius*.8);updateCamera();};document.getElementById('minus').onclick=()=>{radius=Math.min(1100,radius*1.25);updateCamera();};document.getElementById('reset').onclick=reset;document.getElementById('top').onclick=()=>{phi=phi<.1?.85:.025;updateCamera();};
let currentGroundMode='aerial';const modeBtn=document.getElementById('mode');if(modeBtn){modeBtn.onclick=()=>{currentGroundMode=currentGroundMode==='aerial'?'schematic':'aerial';if(window.setGroundMode)window.setGroundMode(currentGroundMode);modeBtn.textContent=currentGroundMode==='aerial'?'实景':'沙盘';modeBtn.classList.toggle('active',currentGroundMode==='aerial');};}
const pointers=new Map();let down=null,moved=false,pinch=0;
const canvas=renderer.domElement;canvas.onpointerdown=e=>{canvas.setPointerCapture(e.pointerId);pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});if(pointers.size===1){down={x:e.clientX,y:e.clientY};moved=false;}else{moved=true;pinch=0;}};
canvas.onpointermove=e=>{const old=pointers.get(e.pointerId);if(!old)return;const dx=e.clientX-old.x,dy=e.clientY-old.y;pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});if(down&&Math.hypot(e.clientX-down.x,e.clientY-down.y)>7)moved=true;
 if(pointers.size===1){theta-=dx*.007;phi=THREE.MathUtils.clamp(phi+dy*.005,.025,1.4);}else{const [a,b]=[...pointers.values()],dist=Math.hypot(a.x-b.x,a.y-b.y);if(pinch)radius=THREE.MathUtils.clamp(radius*pinch/Math.max(dist,1),65,1100);pinch=dist;const k=radius*.001;target.x-=dx*k*Math.cos(theta);target.z+=dx*k*Math.sin(theta);target.z-=dy*k*Math.cos(theta);target.x-=dy*k*Math.sin(theta);target.x=THREE.MathUtils.clamp(target.x,-30,330);target.z=THREE.MathUtils.clamp(target.z,-30,390);}updateCamera();};
function release(e){if(!moved&&pointers.size===1&&down&&e.type!=='pointercancel')pick(e.clientX,e.clientY);pointers.delete(e.pointerId);pinch=0;down=null;}canvas.onpointerup=release;canvas.onpointercancel=release;canvas.onwheel=e=>{e.preventDefault();radius=THREE.MathUtils.clamp(radius*Math.exp(e.deltaY*.001),65,1100);updateCamera();};
window.addEventListener('load',()=>requestAnimationFrame(reset));
window.onresize=()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight);};
document.addEventListener('visibilitychange',()=>paused=document.hidden);window.CampusScene={select:id=>{const b=allPlaces.find(b=>b.id===id);if(b)select(b);},pause:v=>paused=v,buildings:CampusData.buildings,regions:CampusData.regions||[]};
function updateLabels(){
 const candidates=[];const close=radius<480;
 const top=document.getElementById('filters').getBoundingClientRect().bottom+8;
 const bottom=document.querySelector('.card').getBoundingClientRect().top-10;
 const reserved=['.badge','.tools'].map(q=>document.querySelector(q).getBoundingClientRect());
 for(const {b,s}of labels){s.el.style.display='none';if(!s.visible)continue;
  const important=b.region||['library','gym','canteen_main','teach_1','theater_market'].includes(b.id);
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
