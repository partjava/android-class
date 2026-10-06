/**
 * 晴川 3D 校园 · 可视化交互式路网编辑器 (Visual Road Grid Editor)
 * 模式：
 * 1. 两点交点连线：点击起点交点，点击终点交点，自动吸附网格生成笔直道路
 * 2. 涂抹画线：手指按住滑动，沿途网格自动连线
 * 3. 实时生成 3D 沥青路面、路缘石与中心分道线，支持一键保存与导出
 */
(function(root){
 function createRoadEditor(){
  let isEditing=false;
  let drawMode='point'; // 'point' (两点连线) 或 'drag' (涂抹划路)
  let currentWidth=5.0; // 道路宽度
  let roads=[];         // 当前草稿道路列表
  let activePoints=[];  // 当前正在绘制的折线点
  let history=[];       // 撤销历史栈
  let editorGroup=null;
  let gridHelper=null;
  let markerMesh=null;
  let previewGroup=null;
  let raycaster=null;
  let groundPlane=null;
  let sceneRef=null, cameraRef=null, rendererRef=null;

  const GRID_STEP=20; // 网格步长（参考坐标系下每格 20 像素，合世界尺寸 5 米）
  const snap=v=>Math.round(v/GRID_STEP)*GRID_STEP;

  function init({scene, camera, renderer}){
   sceneRef=scene;cameraRef=camera;rendererRef=renderer;
   raycaster=new THREE.Raycaster();
   groundPlane=new THREE.Plane(new THREE.Vector3(0,1,0),0);

   editorGroup=new THREE.Group();
   editorGroup.visible=false;
   scene.add(editorGroup);

   // 1. 创建半透明辅助网格线（覆盖 1320 x 1260 校园沙盘）
   const gridGeo=new THREE.BufferGeometry();
   const gridPts=[];
   for(let x=0;x<=1320;x+=GRID_STEP){
    gridPts.push(x/4,.32,0, x/4,.32,1260/4);
   }
   for(let z=0;z<=1260;z+=GRID_STEP){
    gridPts.push(0,.32,z/4, 1320/4,.32,z/4);
   }
   gridGeo.setAttribute('position',new THREE.Float32BufferAttribute(gridPts,3));
   const gridMat=new THREE.LineBasicMaterial({color:'#4fa88d',transparent:true,opacity:.38});
   gridHelper=new THREE.LineSegments(gridGeo,gridMat);
   editorGroup.add(gridHelper);

   // 2. 当前选中吸附点光圈指示器
   const markerGeo=new THREE.RingGeometry(.7,1.4,16);markerGeo.rotateX(-Math.PI/2);
   const markerMat=new THREE.MeshBasicMaterial({color:'#ffc83b',side:THREE.DoubleSide});
   markerMesh=new THREE.Mesh(markerGeo,markerMat);
   markerMesh.position.y=.42;
   markerMesh.visible=false;
   editorGroup.add(markerMesh);

   // 3. 动态绘制线预览组
   previewGroup=new THREE.Group();
   editorGroup.add(previewGroup);

   // 初始导入现有路网作为草稿
   loadInitialRoads();
   createUI();
   setupPointerEvents();
  }

  function loadInitialRoads(){
   if(window.CampusLayout&&window.CampusLayout.roads){
    roads=JSON.parse(JSON.stringify(window.CampusLayout.roads));
   }
  }

  function updatePreview(){
   while(previewGroup.children.length>0){
    const obj=previewGroup.children[0];
    if(obj.geometry)obj.geometry.dispose();
    previewGroup.remove(obj);
   }
   const W=CampusLayout.world;

   // 渲染已绘制的所有路段
   for(const r of roads){
    const pts=r.points;
    if(!pts||pts.length<2)continue;
    for(let i=1;i<pts.length;i++){
     const a=W(pts[i-1]),b=W(pts[i]);
     const dx=b[0]-a[0],dz=b[1]-a[1],len=Math.hypot(dx,dz);
     if(len<.1)continue;
     const angle=Math.atan2(dx,dz);
     const mx=(a[0]+b[0])/2,mz=(a[1]+b[1])/2;
     const geo=new THREE.BoxGeometry(r.width,.12,len);
     const mat=new THREE.MeshBasicMaterial({color:'#3a4042'});
     const m=new THREE.Mesh(geo,mat);
     m.position.set(mx,.4,mz);m.rotation.y=angle;
     previewGroup.add(m);

     // 中心金黄色预览导引线
     const lineGeo=new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(a[0],.52,a[1]),new THREE.Vector3(b[0],.52,b[1])]);
     const lineMat=new THREE.LineBasicMaterial({color:'#ffd15c',linewidth:2});
     previewGroup.add(new THREE.Line(lineGeo,lineMat));
    }
   }

   // 渲染当前正在画的临时折线
   if(activePoints.length>0){
    for(let i=1;i<activePoints.length;i++){
     const a=W(activePoints[i-1]),b=W(activePoints[i]);
     const lineGeo=new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(a[0],.6,a[1]),new THREE.Vector3(b[0],.6,b[1])]);
     const lineMat=new THREE.LineBasicMaterial({color:'#ff5e3a',linewidth:3});
     previewGroup.add(new THREE.Line(lineGeo,lineMat));
    }
   }
  }

  function getWorldCoords(e){
   const rect=rendererRef.domElement.getBoundingClientRect();
   const x=((e.clientX-rect.left)/rect.width)*2-1;
   const y=-((e.clientY-rect.top)/rect.height)*2+1;
   raycaster.setFromCamera(new THREE.Vector2(x,y),cameraRef);
   const hit=new THREE.Vector3();
   if(raycaster.ray.intersectPlane(groundPlane,hit)){
    const refX=snap(hit.x*4);
    const refZ=snap(hit.z*4);
    if(refX>=0&&refX<=1320&&refZ>=0&&refZ<=1260){
     return [refX,refZ];
    }
   }
   return null;
  }

  let isDragging=false;
  let lastDragPt=null;

  function setupPointerEvents(){
   const dom=rendererRef.domElement;

   dom.addEventListener('pointerdown',e=>{
    if(!isEditing||e.pointerType==='touch'&&e.isPrimary===false)return;
    const pt=getWorldCoords(e);
    if(!pt)return;

    if(drawMode==='point'){
     // 点选模式：点击一个点，再点击下一个点连线
     markerMesh.position.set(pt[0]/4,.42,pt[1]/4);
     markerMesh.visible=true;
     if(activePoints.length===0){
      activePoints.push(pt);
      showToast('已选起点 ('+pt[0]+', '+pt[1]+')，请点击下一个网格交点连线');
     }else{
      const prev=activePoints[activePoints.length-1];
      if(prev[0]!==pt[0]||prev[1]!==pt[1]){
       history.push(JSON.parse(JSON.stringify(roads)));
       roads.push({width:currentWidth,points:[prev,pt]});
       activePoints=[pt];
       updatePreview();
       showToast('已连接道路！继续点击连线，或点击“完成当前路”');
      }
     }
    }else if(drawMode==='drag'){
     // 涂抹模式：按下开始
     isDragging=true;
     history.push(JSON.parse(JSON.stringify(roads)));
     activePoints=[pt];
     lastDragPt=pt;
     updatePreview();
    }
   });

   dom.addEventListener('pointermove',e=>{
    if(!isEditing)return;
    const pt=getWorldCoords(e);
    if(pt){
     markerMesh.position.set(pt[0]/4,.42,pt[1]/4);
     markerMesh.visible=true;
    }
    if(drawMode==='drag'&&isDragging&&pt&&lastDragPt){
     const dist=Math.hypot(pt[0]-lastDragPt[0],pt[1]-lastDragPt[1]);
     if(dist>=GRID_STEP){
      activePoints.push(pt);
      lastDragPt=pt;
      updatePreview();
     }
    }
   });

   dom.addEventListener('pointerup',e=>{
    if(!isEditing)return;
    if(drawMode==='drag'&&isDragging){
     isDragging=false;
     if(activePoints.length>=2){
      roads.push({width:currentWidth,points:[...activePoints]});
      showToast('已生成绘制道路（'+activePoints.length+'个节点）');
     }
     activePoints=[];
     updatePreview();
    }
   });
  }

  function showToast(msg){
   const el=document.getElementById('editor-toast');
   if(el){el.textContent=msg;el.style.opacity='1';clearTimeout(el._t);el._t=setTimeout(()=>{el.style.opacity='0';},2500);}
  }

  function createUI(){
   // 1. 在右侧工具栏添加【画路】按钮
   const tools=document.querySelector('.tools');
   if(tools&&!document.getElementById('road-editor-btn')){
    const btn=document.createElement('button');
    btn.id='road-editor-btn';
    btn.textContent='画路';
    btn.title='开启网格画路编辑面板';
    btn.onclick=toggleEditor;
    tools.insertBefore(btn,document.getElementById('top'));
   }

   // 2. 创建顶部和底部编辑器 HUD 面板
   const hud=document.createElement('div');
   hud.id='road-editor-hud';
   hud.style.cssText='display:none;position:fixed;inset:0;pointer-events:none;z-index:9;font-family:system-ui,-apple-system,sans-serif;';
   hud.innerHTML=`
    <div style="position:absolute;top:10px;left:10px;right:10px;background:rgba(255,255,255,0.92);backdrop-filter:blur(8px);padding:8px 12px;border-radius:12px;box-shadow:0 4px 16px rgba(0,0,0,0.15);pointer-events:auto;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:8px;">
      <div style="display:flex;align-items:center;gap:6px;">
        <span style="font-weight:bold;font-size:13px;color:#1e4d3c;">🛠️ 路网网格编辑器</span>
        <button id="ed-mode-point" style="padding:4px 8px;font-size:11px;border-radius:6px;background:#215e48;color:white;border:0;cursor:pointer;">🔘 两点连线</button>
        <button id="ed-mode-drag" style="padding:4px 8px;font-size:11px;border-radius:6px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">🖌️ 涂抹划线</button>
      </div>
      <div style="display:flex;align-items:center;gap:6px;">
        <span style="font-size:11px;color:#5a6e66;">路宽:</span>
        <button id="ed-w-6" style="padding:4px 7px;font-size:11px;border-radius:6px;background:#215e48;color:white;border:0;cursor:pointer;">主路 6m</button>
        <button id="ed-w-4" style="padding:4px 7px;font-size:11px;border-radius:6px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">次干 4.5m</button>
        <button id="ed-w-3" style="padding:4px 7px;font-size:11px;border-radius:6px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">支路 3m</button>
      </div>
      <div style="display:flex;align-items:center;gap:6px;">
        <button id="ed-finish-active" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#358164;color:white;border:0;cursor:pointer;">结束当前段</button>
        <button id="ed-undo" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#dfd8ce;color:#352e25;border:0;cursor:pointer;">↩ 撤销</button>
        <button id="ed-clear" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#ecdada;color:#8f2d2d;border:0;cursor:pointer;">🗑️ 清空</button>
        <button id="ed-export" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#e2ece7;color:#1b4a39;border:0;cursor:pointer;">📋 导出</button>
        <button id="ed-save" style="padding:5px 12px;font-size:12px;font-weight:bold;border-radius:6px;background:#e37036;color:white;border:0;cursor:pointer;box-shadow:0 2px 8px rgba(227,112,54,0.4);">💾 生成3D路网</button>
        <button id="ed-close" style="padding:5px 10px;font-size:12px;border-radius:6px;background:#444;color:white;border:0;cursor:pointer;">✕ 退出</button>
      </div>
    </div>
    <div id="editor-toast" style="position:absolute;bottom:40px;left:50%;transform:translateX(-50%);background:rgba(20,38,30,0.92);color:white;padding:8px 18px;border-radius:20px;font-size:12px;pointer-events:none;transition:opacity .3s ease;opacity:0;white-space:nowrap;box-shadow:0 4px 14px rgba(0,0,0,0.3);z-index:10;">点击网格交点开始连线</div>
   `;
   document.body.appendChild(hud);

   // 绑定事件
   document.getElementById('ed-mode-point').onclick=()=>{
    drawMode='point';activePoints=[];
    document.getElementById('ed-mode-point').style.background='#215e48';
    document.getElementById('ed-mode-point').style.color='white';
    document.getElementById('ed-mode-drag').style.background='#e5ece8';
    document.getElementById('ed-mode-drag').style.color='#284d40';
    showToast('已切换至【两点交点连线模式】');
   };
   document.getElementById('ed-mode-drag').onclick=()=>{
    drawMode='drag';activePoints=[];
    document.getElementById('ed-mode-drag').style.background='#215e48';
    document.getElementById('ed-mode-drag').style.color='white';
    document.getElementById('ed-mode-point').style.background='#e5ece8';
    document.getElementById('ed-mode-point').style.color='#284d40';
    showToast('已切换至【涂抹连线模式】，按住拖动即可连续画路');
   };

   const setWidth=(w,elId)=>{
    currentWidth=w;
    ['ed-w-6','ed-w-4','ed-w-3'].forEach(id=>{
     const el=document.getElementById(id);
     el.style.background=(id===elId)?'#215e48':'#e5ece8';
     el.style.color=(id===elId)?'white':'#284d40';
    });
   };
   document.getElementById('ed-w-6').onclick=()=>setWidth(6.0,'ed-w-6');
   document.getElementById('ed-w-4').onclick=()=>setWidth(4.5,'ed-w-4');
   document.getElementById('ed-w-3').onclick=()=>setWidth(3.0,'ed-w-3');

   document.getElementById('ed-finish-active').onclick=()=>{
    activePoints=[];
    markerMesh.visible=false;
    updatePreview();
    showToast('已完成当前线段，点击下一个点开始新路段');
   };

   document.getElementById('ed-undo').onclick=()=>{
    if(history.length>0){
     roads=history.pop();
     activePoints=[];
     updatePreview();
     showToast('已撤销一步');
    }
   };

   document.getElementById('ed-clear').onclick=()=>{
    if(confirm('确定要清空当前所有路网重新画吗？')){
     history.push(JSON.parse(JSON.stringify(roads)));
     roads=[];activePoints=[];
     updatePreview();
     showToast('已清空，请在网格上开始画路');
    }
   };

   document.getElementById('ed-export').onclick=()=>{
    const jsonStr=JSON.stringify(roads,null,1);
    if(navigator.clipboard&&navigator.clipboard.writeText){
     navigator.clipboard.writeText(jsonStr).catch(()=>{});
    }
    prompt('当前路网 JSON 数据（可复制保存到代码）：', jsonStr);
   };

   document.getElementById('ed-save').onclick=()=>{
    if(roads.length===0){alert('还没有画任何道路哦，请先在格子上连线！');return;}
    if(window.rebuildCampusRoads){
     window.rebuildCampusRoads(roads);
     showToast('🎉 3D 沥青道路已实时生成成功！');
    }
    toggleEditor();
   };

   document.getElementById('ed-close').onclick=toggleEditor;
  }

  function toggleEditor(){
   isEditing=!isEditing;
   const hud=document.getElementById('road-editor-hud');
   const btn=document.getElementById('road-editor-btn');
   if(hud)hud.style.display=isEditing?'block':'none';
   if(btn){
    btn.classList.toggle('active',isEditing);
    btn.textContent=isEditing?'退出':'画路';
   }
   if(editorGroup)editorGroup.visible=isEditing;

   const toHide=['.top','.filters','.badge','.card','#labels','.compass','.hint'];
   toHide.forEach(sel=>{
    const el=document.querySelector(sel);
    if(el)el.style.display=isEditing?'none':'';
   });

   if(isEditing){
    // 进入编辑模式：俯视居中，显示航拍底图便于对齐白线
    loadInitialRoads();
    updatePreview();
    if(window.setGroundMode)window.setGroundMode('aerial');
    const modeBtn=document.getElementById('mode');
    if(modeBtn){modeBtn.textContent='实景';modeBtn.classList.add('active');}
    if(window.resetCameraTop)window.resetCameraTop();
    showToast('已进入路网绘制面板：可点选两点相交连线，或拖动涂抹画路');
   }else{
    if(markerMesh)markerMesh.visible=false;
    activePoints=[];
   }
  }

  root.RoadEditor={init,toggleEditor,isEditing:()=>isEditing};
 }

 if(typeof module!=='undefined')module.exports=createRoadEditor;
 else createRoadEditor();
})(typeof window!=='undefined'?window:globalThis);
