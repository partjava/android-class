/**
 * 晴川 3D 校园 · 可视化交互式路网网格编辑器 (Visual Road Grid Editor)
 * 功能特性：
 * 1. 🖐️ 移动视野：单指顺畅滑动平移整个校园地图，双指捏合缩放
 * 2. 🔘 两点连线：点击起点与终点，自动吸附20像素(5米)正交网格生成笔直道路
 * 3. 🖌️ 涂抹划线：按住手指连续滑动，沿途网格自动平滑铺设道路
 * 4. 🧹 橡皮擦：点选或划过任意已画道路，即可精准擦除该段道路
 * 5. 宽度与材质：主路 6m (深灰沥青)、次干 4.5m (中灰沥青)、白板路 3m (米白色石板人行道)
 * 6. 可折叠面板：一键收起为极简顶部浮标，展开可操作撤销/清空/导出/3D生成
 */
(function(root){
 function createRoadEditor(){
  let isEditing=false;
  let isCollapsed=false;
  let drawMode='point'; // 'pan' (移动视野), 'point' (两点连线), 'drag' (涂抹划路), 'erase' (橡皮擦)
  let currentWidth=6.0; // 道路宽度：6.0 (主路), 4.5 (次干), 3.0 (白板路)
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

  const GRID_STEP=20; // 网格步长（参考坐标系下每格 20 像素，对应世界尺寸 5 米）
  const snap=v=>Math.round(v/GRID_STEP)*GRID_STEP;

  function init({scene, camera, renderer}){
   sceneRef=scene;cameraRef=camera;rendererRef=renderer;
   raycaster=new THREE.Raycaster();
   groundPlane=new THREE.Plane(new THREE.Vector3(0,1,0),0);

   editorGroup=new THREE.Group();
   editorGroup.visible=false;
   scene.add(editorGroup);

   // 1. 创建半透明辅助正交网格线（覆盖 1320 x 1260 校园沙盘，每格5米）
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

   loadInitialRoads();
   createUI();
   setupPointerEvents();
  }

  
  function autoSaveRoads(){
   if(window.CampusNativeBridge && typeof window.CampusNativeBridge.saveConfig === 'function'){
    try{ window.CampusNativeBridge.saveConfig('custom_campus_roads', JSON.stringify(roads)); }catch(e){}
   }
   try{
    if(typeof localStorage !== 'undefined' && localStorage){
     localStorage.setItem('custom_campus_roads', JSON.stringify(roads));
    }
   }catch(e){}
  }

  function loadInitialRoads(){
   if(window.CampusNativeBridge && typeof window.CampusNativeBridge.loadConfig === 'function'){
    try{
     const nativeStr = window.CampusNativeBridge.loadConfig('custom_campus_roads');
     if(nativeStr){ roads = JSON.parse(nativeStr); return; }
    }catch(e){}
   }
   try{
    if(typeof localStorage !== 'undefined' && localStorage){
     const stored = localStorage.getItem('custom_campus_roads');
     if(stored !== null){ roads = JSON.parse(stored); return; }
    }
   }catch(e){}
   if(window.CampusLayout&&window.CampusLayout.roads&&window.CampusLayout.roads.length>0){
    roads=JSON.parse(JSON.stringify(window.CampusLayout.roads));
   }else{
    roads=[];
   }
  }

  function updatePreview(){
   while(previewGroup.children.length>0){
    const obj=previewGroup.children[0];
    if(obj.geometry)obj.geometry.dispose();
    previewGroup.remove(obj);
   }
   const W=(window.CampusLayout&&window.CampusLayout.world)?window.CampusLayout.world:p=>[p[0]/4,p[1]/4];

   // 渲染已绘制的所有路段
   for(const r of roads){
    const pts=r.points;
    if(!pts||pts.length<2)continue;
    const isWhitePaved=(r.width<=3.2); // 3m 支路为米白色白板路
    const roadColor=isWhitePaved?'#eae5dc':'#3a4042';
    const lineColor=isWhitePaved?'#c5bdae':'#ffd15c';

    for(let i=1;i<pts.length;i++){
     const a=W(pts[i-1]),b=W(pts[i]);
     const dx=b[0]-a[0],dz=b[1]-a[1],len=Math.hypot(dx,dz);
     if(len<.1)continue;
     const angle=Math.atan2(dx,dz);
     const mx=(a[0]+b[0])/2,mz=(a[1]+b[1])/2;
     const geo=new THREE.BoxGeometry(r.width,.12,len);
     const mat=new THREE.MeshBasicMaterial({color:roadColor});
     const m=new THREE.Mesh(geo,mat);
     m.position.set(mx,.4,mz);m.rotation.y=angle;
     previewGroup.add(m);

     // 中心导引线（主路金黄色，白板路米灰线）
     const lineGeo=new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(a[0],.52,a[1]),new THREE.Vector3(b[0],.52,b[1])]);
     const lineMat=new THREE.LineBasicMaterial({color:lineColor,linewidth:2});
     previewGroup.add(new THREE.Line(lineGeo,lineMat));
    }
   }

   // 渲染当前正在连线的临时活动线段
   if(activePoints.length>0){
    for(let i=1;i<activePoints.length;i++){
     const a=W(activePoints[i-1]),b=W(activePoints[i]);
     const lineGeo=new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(a[0],.6,a[1]),new THREE.Vector3(b[0],.6,b[1])]);
     const lineMat=new THREE.LineBasicMaterial({color:'#ff5e3a',linewidth:3});
     previewGroup.add(new THREE.Line(lineGeo,lineMat));
    }
   }
   updateCollapsedLabel();
  }

  function getWorldCoords(e){
   if(!rendererRef||!cameraRef)return null;
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

  // 橡皮擦功能：擦除指定交点附近的路段
  function eraseNear(pt){
   if(!pt||roads.length===0)return false;
   let erased=false;
   const remainingRoads=[];
   for(const r of roads){
    const pts=r.points;
    let hit=false;
    for(let i=1;i<pts.length;i++){
     const p1=pts[i-1],p2=pts[i];
     const d1=Math.hypot(pt[0]-p1[0],pt[1]-p1[1]);
     const d2=Math.hypot(pt[0]-p2[0],pt[1]-p2[1]);
     if(d1<=GRID_STEP*1.1||d2<=GRID_STEP*1.1){
      hit=true;break;
     }
     const l2=(p2[0]-p1[0])**2+(p2[1]-p1[1])**2;
     if(l2>0){
      let t=((pt[0]-p1[0])*(p2[0]-p1[0])+(pt[1]-p1[1])*(p2[1]-p1[1]))/l2;
      t=Math.max(0,Math.min(1,t));
      const projX=p1[0]+t*(p2[0]-p1[0]);
      const projZ=p1[1]+t*(p2[1]-p1[1]);
      const dist=Math.hypot(pt[0]-projX,pt[1]-projZ);
      if(dist<=(r.width*4/2)+GRID_STEP*0.8){
       hit=true;break;
      }
     }
    }
    if(hit)erased=true;
    else remainingRoads.push(r);
   }

   if(erased){
    history.push(JSON.parse(JSON.stringify(roads)));
    roads=remainingRoads;
    updatePreview();
    autoSaveRoads();
    showToast('🧹 已精准擦除触碰的道路');
    return true;
   }
   return false;
  }

  let isDragging=false;
  let lastDragPt=null;

  function setupPointerEvents(){
   const dom=rendererRef.domElement;

   dom.addEventListener('pointerdown',e=>{
    if(!isEditing)return;
    if(e.pointerType==='touch'&&e.isPrimary===false)return;
    if(drawMode==='pan')return; // 移动模式由 campus-scene.js 处理单指平移

    const pt=getWorldCoords(e);
    if(!pt)return;

    if(drawMode==='erase'){
     // 橡皮擦模式：按下直接擦除
     isDragging=true;
     eraseNear(pt);
     markerMesh.position.set(pt[0]/4,.42,pt[1]/4);
     markerMesh.visible=true;
    }else if(drawMode==='point'){
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
       autoSaveRoads();
       showToast('已连接道路！继续点击连线，或点击“结束当前段”');
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
    if(!isEditing||drawMode==='pan')return;
    const pt=getWorldCoords(e);
    if(pt){
     markerMesh.position.set(pt[0]/4,.42,pt[1]/4);
     markerMesh.visible=true;
    }
    if(drawMode==='erase'&&isDragging&&pt){
     eraseNear(pt);
    }else if(drawMode==='drag'&&isDragging&&pt&&lastDragPt){
     const dist=Math.hypot(pt[0]-lastDragPt[0],pt[1]-lastDragPt[1]);
     if(dist>=GRID_STEP){
      activePoints.push(pt);
      lastDragPt=pt;
      updatePreview();
     }
    }
   });

   dom.addEventListener('pointerup',e=>{
    if(!isEditing||drawMode==='pan')return;
    if(drawMode==='drag'&&isDragging){
     isDragging=false;
     if(activePoints.length>=2){
      roads.push({width:currentWidth,points:[...activePoints]});
      showToast('已生成绘制道路（'+activePoints.length+'个节点）');
     }
     activePoints=[];
     updatePreview();
     autoSaveRoads();
    }else if(drawMode==='erase'){
     isDragging=false;
    }
   });
  }

  function showToast(msg){
   const el=document.getElementById('editor-toast');
   if(el){
    el.textContent=msg;
    el.style.opacity='1';
    clearTimeout(el._t);
    el._t=setTimeout(()=>{el.style.opacity='0';},2500);
   }
  }

  function getModeName(){
   if(drawMode==='pan')return '🖐️ 移动视野';
   if(drawMode==='point')return '🔘 两点连线';
   if(drawMode==='drag')return '🖌️ 涂抹划线';
   if(drawMode==='erase')return '🧹 橡皮擦';
   return '';
  }

  function getWidthName(){
   if(currentWidth===6.0)return '主路 6m';
   if(currentWidth===4.5)return '次干 4.5m';
   return '白板路 3m';
  }

  function updateCollapsedLabel(){
   const lbl=document.getElementById('ed-collapsed-label');
   if(lbl){
    lbl.textContent=getModeName()+' · '+getWidthName()+' ('+roads.length+'段)';
   }
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

   // 2. 创建顶部和底部编辑器 HUD 面板（支持收起与展开）
   const hud=document.createElement('div');
   hud.id='road-editor-hud';
   hud.style.cssText='display:none;position:fixed;inset:0;pointer-events:none;z-index:9;font-family:system-ui,-apple-system,sans-serif;';
   hud.innerHTML=`
    <!-- 展开状态完整工具箱 -->
    <div id="ed-panel-full" style="position:absolute;top:10px;left:10px;right:10px;background:rgba(255,255,255,0.95);backdrop-filter:blur(10px);padding:10px 12px;border-radius:14px;box-shadow:0 6px 20px rgba(0,0,0,0.18);pointer-events:auto;display:flex;flex-direction:column;gap:8px;">
      <!-- 第一行：标题 + 展开收起切换 + 退出 -->
      <div style="display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e2ebe6;padding-bottom:6px;">
        <div style="display:flex;align-items:center;gap:6px;">
          <span style="font-weight:bold;font-size:13px;color:#1e4d3c;">🛠️ 路网网格编辑器</span>
          <span style="font-size:10px;background:#e5f0eb;color:#215e48;padding:2px 6px;border-radius:4px;">5m吸附网格</span>
        </div>
        <div style="display:flex;align-items:center;gap:6px;">
          <button id="ed-collapse-btn" style="padding:4px 9px;font-size:11px;border-radius:6px;background:#e2eeea;color:#1d4e3d;border:0;cursor:pointer;font-weight:bold;">▲ 收起面板</button>
          <button id="ed-close-btn" style="padding:4px 9px;font-size:11px;border-radius:6px;background:#444;color:white;border:0;cursor:pointer;">✕ 退出</button>
        </div>
      </div>

      <!-- 第二行：操作模式选择 -->
      <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap;">
        <span style="font-size:11px;font-weight:bold;color:#456657;">模式:</span>
        <button id="ed-mode-pan" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">🖐️ 移动视野</button>
        <button id="ed-mode-point" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#215e48;color:white;border:0;cursor:pointer;">🔘 两点连线</button>
        <button id="ed-mode-drag" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">🖌️ 涂抹划线</button>
        <button id="ed-mode-erase" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#ecdada;color:#8f2d2d;border:0;cursor:pointer;">🧹 橡皮擦</button>
      </div>

      <!-- 第三行：道路宽度与类型选择 -->
      <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap;">
        <span style="font-size:11px;font-weight:bold;color:#456657;">路型:</span>
        <button id="ed-w-6" style="padding:5px 8px;font-size:11px;border-radius:6px;background:#215e48;color:white;border:0;cursor:pointer;">主路 6m</button>
        <button id="ed-w-4" style="padding:5px 8px;font-size:11px;border-radius:6px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">次干 4.5m</button>
        <button id="ed-w-3" style="padding:5px 8px;font-size:11px;border-radius:6px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;border:1px solid #ced5cb;">白板路 3m</button>
        <span style="font-size:10px;color:#788b83;margin-left:2px;">(白板路为米白色石板人行道)</span>
      </div>

      <!-- 第四行：编辑控制与一键生成 -->
      <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:6px;padding-top:4px;border-top:1px dashed #e2ebe6;">
        <div style="display:flex;align-items:center;gap:5px;">
          <button id="ed-finish-active" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#358164;color:white;border:0;cursor:pointer;">结束当前段</button>
          <button id="ed-undo" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#dfd8ce;color:#352e25;border:0;cursor:pointer;">↩ 撤销</button>
          <button id="ed-clear" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#ecdada;color:#8f2d2d;border:0;cursor:pointer;">🗑️ 清空所有</button>
          <button id="ed-export" style="padding:5px 9px;font-size:11px;border-radius:6px;background:#e2ece7;color:#1b4a39;border:0;cursor:pointer;">📋 导出</button>
        </div>
        <button id="ed-save" style="padding:6px 14px;font-size:12px;font-weight:bold;border-radius:8px;background:#e37036;color:white;border:0;cursor:pointer;box-shadow:0 3px 10px rgba(227,112,54,0.45);">💾 生成3D路网</button>
      </div>
    </div>

    <!-- 收起状态精简浮标小药丸（超小占用，不挡屏幕画路） -->
    <div id="ed-panel-mini" style="display:none;position:absolute;top:10px;left:10px;background:rgba(255,255,255,0.92);backdrop-filter:blur(8px);padding:6px 14px;border-radius:24px;box-shadow:0 4px 14px rgba(0,0,0,0.2);pointer-events:auto;align-items:center;gap:10px;">
      <span style="font-weight:bold;font-size:12px;color:#1e4d3c;">🛠️ 画路中</span>
      <span id="ed-collapsed-label" style="font-size:11px;color:#3e6354;">🔘 两点连线 · 主路 6m</span>
      <button id="ed-expand-btn" style="padding:3px 10px;font-size:11px;font-weight:bold;border-radius:12px;background:#215e48;color:white;border:0;cursor:pointer;">▼ 展开面板</button>
    </div>

    <!-- 底部操作提示 -->
    <div id="editor-toast" style="position:absolute;bottom:40px;left:50%;transform:translateX(-50%);background:rgba(20,38,30,0.92);color:white;padding:8px 18px;border-radius:20px;font-size:12px;pointer-events:none;transition:opacity .3s ease;opacity:0;white-space:nowrap;box-shadow:0 4px 14px rgba(0,0,0,0.3);z-index:10;">点击网格交点开始连线</div>
   `;
   document.body.appendChild(hud);

   // 绑定展开/收起事件
   const panelFull=document.getElementById('ed-panel-full');
   const panelMini=document.getElementById('ed-panel-mini');
   const setCollapse=(collapsed)=>{
    isCollapsed=collapsed;
    panelFull.style.display=collapsed?'none':'flex';
    panelMini.style.display=collapsed?'flex':'none';
    updateCollapsedLabel();
   };
   document.getElementById('ed-collapse-btn').onclick=()=>setCollapse(true);
   document.getElementById('ed-expand-btn').onclick=()=>setCollapse(false);

   // 绑定模式切换
   const setMode=(mode)=>{
    drawMode=mode;
    activePoints=[];
    ['ed-mode-pan','ed-mode-point','ed-mode-drag','ed-mode-erase'].forEach(id=>{
     const el=document.getElementById(id);
     const isActive=(id==='ed-mode-'+mode);
     if(id==='ed-mode-erase'){
      el.style.background=isActive?'#8f2d2d':'#ecdada';
      el.style.color=isActive?'white':'#8f2d2d';
     }else{
      el.style.background=isActive?'#215e48':'#e5ece8';
      el.style.color=isActive?'white':'#284d40';
     }
    });
    if(markerMesh)markerMesh.visible=(mode!=='pan');
    updateCollapsedLabel();
    if(mode==='pan')showToast('🖐️ 移动视野模式：单指滑动即可平移整个校园地图');
    else if(mode==='point')showToast('🔘 两点连线模式：点击起点交点，再点击终点生成道路');
    else if(mode==='drag')showToast('🖌️ 涂抹连线模式：按住拖动即可连续铺路');
    else if(mode==='erase')showToast('🧹 橡皮擦模式：点击或划过任意已画道路即可精准擦除');
   };

   document.getElementById('ed-mode-pan').onclick=()=>setMode('pan');
   document.getElementById('ed-mode-point').onclick=()=>setMode('point');
   document.getElementById('ed-mode-drag').onclick=()=>setMode('drag');
   document.getElementById('ed-mode-erase').onclick=()=>setMode('erase');

   // 绑定路宽与白板路切换
   const setWidth=(w,elId)=>{
    currentWidth=w;
    ['ed-w-6','ed-w-4','ed-w-3'].forEach(id=>{
     const el=document.getElementById(id);
     el.style.background=(id===elId)?'#215e48':'#e5ece8';
     el.style.color=(id===elId)?'white':'#284d40';
    });
    updateCollapsedLabel();
    if(w<=3.2)showToast('已选【白板路 3m】：米白色石板人行道材质');
    else showToast('已选路宽 '+w+'m：深灰沥青机动车道材质');
   };
   document.getElementById('ed-w-6').onclick=()=>setWidth(6.0,'ed-w-6');
   document.getElementById('ed-w-4').onclick=()=>setWidth(4.5,'ed-w-4');
   document.getElementById('ed-w-3').onclick=()=>setWidth(3.0,'ed-w-3');

   // 结束当前折线
   document.getElementById('ed-finish-active').onclick=()=>{
    activePoints=[];
    if(markerMesh)markerMesh.visible=false;
    updatePreview();
    showToast('已结束当前线段，点击下一个点开始新路段');
   };

   // 撤销一步
   document.getElementById('ed-undo').onclick=()=>{
    if(history.length>0){
     roads=history.pop();
     activePoints=[];
     updatePreview();
     autoSaveRoads();
     showToast('↩ 已撤销一步操作');
    }else{
     showToast('没有可撤销的步骤了');
    }
   };

   // 清空所有道路
   document.getElementById('ed-clear').onclick=()=>{
    if(confirm('确定要清空画布上的所有道路吗？(可随时点撤销恢复)')){
     history.push(JSON.parse(JSON.stringify(roads)));
     roads=[];activePoints=[];
     updatePreview();
     autoSaveRoads();
     showToast('🗑️ 画布已清空，请在网格上开始画路');
    }
   };

   // 导出道路坐标 JSON
   document.getElementById('ed-export').onclick=()=>{
    const jsonStr=JSON.stringify(roads,null,1);
    if(window.showExportModal) window.showExportModal('📋 道路路网数据导出 (' + roads.length + ' 段)', jsonStr);
    else prompt('当前绘制路网 JSON 数据：', jsonStr);
   };

   // 保存并实时生成 3D 沥青/白板路网
   document.getElementById('ed-save').onclick=()=>{
    if(window.rebuildCampusRoads){
     window.rebuildCampusRoads(roads);
     showToast('🎉 3D 路网已实时更新！共生成 '+roads.length+' 段道路');
    }
    toggleEditor();
   };

   document.getElementById('ed-close-btn').onclick=toggleEditor;
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

   // 编辑模式下隐藏周围可能遮挡的搜索栏、标签和底卡
   const toHide=['.top','.filters','.badge','.card','#labels','.compass','.hint'];
   toHide.forEach(sel=>{
    const el=document.querySelector(sel);
    if(el)el.style.display=isEditing?'none':'';
   });

   if(isEditing){
    loadInitialRoads();
    updatePreview();
    if(window.setGroundMode)window.setGroundMode('aerial');
    const modeBtn=document.getElementById('mode');
    if(modeBtn){modeBtn.textContent='实景';modeBtn.classList.add('active');}
    if(window.resetCameraTop)window.resetCameraTop();
    showToast('已进入路网绘制面板：可移动视野、两点连线、连续涂抹或橡皮擦');
   }else{
    if(markerMesh)markerMesh.visible=false;
    activePoints=[];
   }
  }

  root.RoadEditor={
   init,
   toggleEditor,
   isEditing:()=>isEditing,
   getMode:()=>drawMode,
   clearRoads:()=>{history.push(JSON.parse(JSON.stringify(roads)));roads=[];activePoints=[];updatePreview();},
   getRoads:()=>roads
  };
 }

 if(typeof module!=='undefined')module.exports=createRoadEditor;
 else createRoadEditor();
})(typeof window!=='undefined'?window:globalThis);
